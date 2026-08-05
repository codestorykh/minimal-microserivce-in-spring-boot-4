# Money DDD — Multi-Basis Fee & Commission Rule Engine

This module calculates FX, transaction fees, commissions, and channel commissions for cross-currency payments (mainly **USD ↔ KHR**).

The hard problem: **some products price on sender currency, some on receiver currency, and some on the instruction (txn) currency.**  
This README explains how to handle that cleanly.

---

## 1. The challenge

| Service example | Amount customers talk about | Fee usually based on |
|---|---|---|
| P2P / Remittance | “Send $100” | **Sender** (USD) |
| Bill payment | “Pay 400,000 KHR” | **Receiver** (KHR) |
| Merchant QR | Either side, product-dependent | **Txn / Sender / Receiver** |
| Channel commission | Share of fee or txn | Often **% of fee** or **txn** |

If Java code hardcodes “bill = receiver, p2p = sender”, the system becomes brittle.  
**Put currency basis on each rule**, not inside each service class forever.

---

## 2. Design principle

> Normalize amounts first. Let every charge rule declare its **basis**.

Every charge rule answers:

1. **What** — `SERVICE_FEE` / `COMMISSION` / `CHANNEL_COMMISSION`
2. **Which amount** — `SENDER` / `RECEIVER` / `TXN`
3. **Which currency** — fee currency (USD / KHR / …)
4. **Who pays / who earns** — customer / bank / agent / channel

---

## 3. Canonical pricing context

Build one normalized context before rules run:

| Field | Meaning | Example |
|---|---|---|
| `serviceType` | Product code | `P2P_TRANSFER`, `BILL_PAYMENT`, `REMITTANCE` |
| `senderCcy` / `senderAmount` | Debit-side money | `100.00 USD` |
| `receiverCcy` / `receiverAmount` | Credit-side money | `404000 KHR` |
| `txnCcy` / `txnAmount` | Instruction amount | Bill quoted in KHR, P2P quoted in USD |
| `fxRate` | Applied rate | `4040` |
| `channel` | Delivery channel | `APP`, `AGENT`, `API` |
| `customerTier` | Pricing tier | `RETAIL`, `VIP` |

### Resolving txn currency

Never assume txn = sender.

| Product intent | Resolution |
|---|---|
| Send fixed source amount | `txn = sender`, derive receiver via FX |
| Pay fixed target amount (bill) | `txn = receiver`, derive sender via FX |
| Explicit instruction currency | Use `txnCurrency` + `txnAmount`, derive the other side |

In code today this maps roughly to `CalculationContext`:

- `sourceCurrency` / `targetCurrency`
- `txnCurrency` / `txnAmount`
- `serviceType`, `customerTier`

---

## 4. Currency basis

Use a first-class basis enum (already exists as `CurrencyBasis`):

| Basis | Use when |
|---|---|
| `SENDER_CURRENCY` | % or flat fee on debit amount (e.g. `$0.50`, `0.5%` of USD) |
| `RECEIVER_CURRENCY` | % or flat fee on credit amount (e.g. `1,000 KHR` clearing/biller fee) |
| `TXN` *(recommended addition)* | Fee tied to instruction amount regardless of which account is debited |
| `EQUIVALENT_USD` | Global limits / tier checks in one reporting currency |

DMN currently uses labels like `SOURCE` / `TARGET` — keep them aligned with this enum.

---

## 5. Recommended charge line (rule output)

Each matched rule should produce a structured line:

```text
charge_code          e.g. SERVICE_FEE, BILLER_FEE, CLEARING_FEE, AGENT_COMMISSION
charge_type          FEE | COMMISSION | CHANNEL_COMMISSION
basis                SENDER | RECEIVER | TXN
amount               numeric
currency             USD | KHR | ...
collect_from         SENDER | RECEIVER | NONE
payout_to            BANK | AGENT | CHANNEL | NONE
```

### Examples

| Rule | Basis | Amount | Collect from | Payout to |
|---|---|---|---|---|
| Remittance service fee | SENDER | `1.00 USD` | SENDER | BANK |
| Clearing fee | RECEIVER | `1000 KHR` | SENDER (converted) | BANK |
| Biller fee | RECEIVER | `1000 KHR` | SENDER (converted) | BILLER/CHANNEL |
| Agent commission | % of SERVICE_FEE | `20%` | NONE | AGENT |
| Channel commission | % of TXN or FEE | product-specific | NONE | CHANNEL |

Always keep **both**:

- native fee (`1000 KHR`)
- customer-charged equivalent (`0.25 USD` at rate 4040)

---

## 6. Calculation pipeline

```text
Txn request
   │
   ▼
1) Normalize amounts + apply FX
   → senderMoney, receiverMoney, txnMoney, fxRate
   │
   ▼
2) Evaluate rules (Hit Policy: COLLECT)
   → many charge lines can match
   │
   ▼
3) Apply each charge against its basis amount
   │
   ▼
4) Convert charges to debit currency when collect_from = SENDER
   │
   ▼
5) Totals
   - customer debit total
   - receiver credit
   - bank revenue / agent / channel payouts
```

### Order matters

1. **FX before fees** if any rule needs receiver amount  
2. **Fee/commission after FX**  
3. **Convert only for collection/reporting**, never lose original fee currency  

---

## 7. How to model services without special-casing forever

### Good

- Service strategy selects product flow / defaults
- DMN (or DB rules) owns amounts, tiers, and **basis**
- Shared calculator applies lines by basis

### Avoid

- `if (BILL_PAYMENT) useReceiverElseSender()` scattered in many classes
- Mixing bare `BigDecimal` values from different currencies
- Evaluating receiver-based fees before FX exists

Current strategy classes (`P2PTransferPricingStrategy`, `BillPaymentPricingStrategy`) are fine as **orchestrators**.  
Long-term, push fee/commission numbers into rules keyed by `basis`.

---

## 8. DMN shape (current + target)

Existing fee table (`rules/usd_khr_rules.dmn`) already has:

**Inputs**

- `service_type`
- `source_amount_usd`
- `target_amount_khr`

**Outputs**

- `fee_code`
- `currency_basis` (`SOURCE` / `TARGET`)
- `fee_amount`
- `fee_currency`

### Suggested evolution

Add inputs:

- `txn_amount`
- `txn_currency`
- `channel`
- `customer_tier`

Add outputs:

- `charge_type` (`FEE` / `COMMISSION` / `CHANNEL_COMMISSION`)
- `collect_from`
- `payout_to`

Keep hit policy **`COLLECT`** so one transaction can produce multiple charge lines.

---

## 9. Worked example — Remittance $100 USD → KHR

Assume FX = `4040`.

| Step | Value |
|---|---|
| Sender | `100.00 USD` |
| Receiver | `404,000 KHR` |
| Service fee (SENDER) | `1.00 USD` |
| Clearing fee (RECEIVER) | `1,000 KHR` ≈ `0.25 USD` |
| Customer debit | `100 + 1.00 + 0.25 = 101.25 USD` |
| Receiver credit | `404,000 KHR` (usually clean principal) |
| Agent commission (20% of service fee) | `0.20 USD` payout |

Same engine, two bases on two fee rows.

---

## 10. Worked example — Bill payment quoted in KHR

Customer pays a `400,000 KHR` bill from a USD account.

| Step | Value |
|---|---|
| Txn / receiver | `400,000 KHR` |
| Derived sender (FX 4040) | `99.01 USD` |
| Biller fee (RECEIVER) | `1,000 KHR` ≈ `0.25 USD` |
| Customer debit | `99.01 + 0.25 USD` (product-dependent) |
| Biller credit | `400,000 KHR` |

Basis = **RECEIVER** even though customer account is USD.

---

## 11. Module map (current code)

```text
money-ddd/
├── domain/
│   ├── Money              # amount + currency value object
│   ├── Account            # local balances for processPayment
│   ├── FxRate
│   └── FeeConfig
├── repository/
│   ├── AccountRepository
│   ├── FxRateRepository
│   └── FeeConfigRepository
├── service/
│   ├── DmnRuleEngineService          # Camunda DMN runtime
│   ├── PaymentCalculationService     # DMN quote path
│   ├── PaymentPricingOrchestrator    # strategy dispatch
│   ├── P2PTransferPricingStrategy    # sender-basis example
│   ├── BillPaymentPricingStrategy    # receiver-basis example
│   ├── TransactionService            # validate + debit/credit
│   └── validators...
├── dto/
│   ├── CalculationContext
│   ├── CalculationResult
│   └── FeeBreakdown
├── type/
│   └── CurrencyBasis
└── resources/rules/
    └── usd_khr_rules.dmn
```

### HTTP today

- `POST /api/v1/payments/calculate` — quote FX + DMN fees

---

## 12. Implementation checklist

- [x] `Money` value object (never mix currencies silently)
- [x] `CurrencyBasis` enum
- [x] DMN fee table with basis + fee currency
- [x] Strategy split for P2P vs Bill
- [ ] Add explicit `TXN` basis everywhere (enum + DMN)
- [ ] Unified `ChargeLine` for fee / commission / channel commission
- [ ] Normalize sender/receiver/txn amounts in one shared builder
- [ ] Convert-to-sender helper for `collect_from = SENDER`
- [ ] Persist charge breakdown on each processed payment

---

## 13. Testing guidance

For each product, assert:

1. Correct **basis amount** used  
2. Fee kept in **native currency**  
3. Customer charge converted correctly when needed  
4. Receiver credit not accidentally reduced unless product requires it  
5. Sanity guardrail still blocks corrupted FX math  

Existing tests:

- `BillPaymentPricingStrategyTest`
- `P2PTransferPricingStrategyTest`
- `PaymentCalculationServiceTest`
- `DmnRuleEngineServiceTest`
- `PaymentFlowIntegrationTest`

---

## 14. Bottom line

**Normalize three moneys (`sender`, `receiver`, `txn`) once.**  
**Declare basis on every fee/commission rule.**  
**Convert only at collection/reporting time.**

That is how one rule engine supports mixed-currency products without drowning in service-specific `if` branches.
