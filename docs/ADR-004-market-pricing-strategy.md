# Market Pricing Strategy — Architectural Decision Record (ADR-004)

## 📘 Context

This document records the architectural decision regarding the use of market prices within the Energy Trade Platform, specifically as it relates to when and how these prices are fetched and utilized during the order lifecycle.

## 🎯 Goal

To define a consistent, performant, and auditable strategy for acquiring and applying market prices to orders — both during their creation and potential execution phases — with considerations for system design, industry practices, and future evolution.

## 🧩 Problem Statement

In the current architecture, a dedicated `pricing-service` is responsible for fetching the market price from an external source (`externalgw-service`). A decision was needed on:

- Whether to fetch the price only once at order creation (`marketPrice`)
- Or whether the matching engine should dynamically query the latest market price during execution

Each approach carries trade-offs in complexity, performance, and system behavior.

## ⚖️ Alternatives Considered

### Option 1: Use price at order creation (Chosen ✅)

- The `marketPrice` is fetched from `pricing-service` only when the order is created.
- This value is stored in the order and used for reporting, auditing, and market impact estimations.

**Pros:**
- High performance: no external calls during matching
- Deterministic behavior: no price drift between creation and execution
- Easier to test, debug, and reproduce historical scenarios
- Common in real-world trading platforms (crypto, equities, futures)

**Cons:**
- May not reflect the most up-to-date market price if there is a time gap between order creation and execution

### Option 2: Query live price during matching ❌

- The matching engine would call `pricing-service` during order matching to get the real-time price

**Pros:**
- More accurate at the time of execution
- Enables dynamic rules such as slippage tolerance or price-sensitive matching

**Cons:**
- Adds latency and coupling between services
- Risk of inconsistency if `pricing-service` is unavailable
- Complicates testing and scalability of the matching engine


## 🏛 Industry Benchmark Verification

The following statements describe common practices in real-world electronic trading platforms. They have been researched, and supporting references (including primary sources) are provided.

### 1. Exchanges **avoid calling external pricing services during core matching**
Trading venues (e.g., Binance, NYSE, CME) design their matching engines to operate on **static order book data**, without any dependencies on external pricing services during execution:

- **Binance’s matching engine** processes buy/sell orders solely based on internal order book logic (price & time priority), without external calls[^1].  
- **CME Globex** runs high-performance matching servers embedded within its datacenter – colocated, ultra-low-latency, and decoupled from external pricing feeds[^2].

### 2. They rely on **limit prices submitted with orders**
Orders carry their own limit prices; matching engines use these to find compatible counterparts:

- Binance, CME, and others primarily use **price-time priority**, where limit orders dominate decision logic[^3][^4].

### 3. Market price snapshots are captured separately (analytics/display)
- Market data feeds are handled **asynchronously**, not within the matching engine core.  
- These snapshots are used in dashboards, UIs, and analytics systems, separate from actual matching logic[^5].

### 4. External pricing feeds are processed asynchronously
- Exchanges ingest live data via subscribing to external/own market feeds, but these run **independently** from core order matching, for analytics or user display.  
- For example, CME and others operate **distinct feed handlers**, separated from matching services[^6].

---

### ✅ Summary Table

| Claim                                      | Verified? | Source(s)       |
|-------------------------------------------|-----------|-----------------|
| No external calls during matching         | ✅ Yes    | [^1], [^2]      |
| Limit orders drive matching               | ✅ Yes    | [^3], [^4]      |
| Market data feeds are async               | ✅ Yes    | [^5]            |
| Pricing feeds are separate from matching  | ✅ Yes    | [^6]            |

---

## ✍️ Conclusion

These industry-standard practices confirm that **avoiding external service calls during matching** and **using stored market snapshots at order creation** is not only valid—it is standard and preferred in modern electronic trading systems.

Including this verified rationale (with footnotes) reinforces the architectural soundness of our **market-price-at-order-creation** decision.

---

## 📎 Footnotes

[^1]: “Binance’s algorithmic matching engines ingest incoming buy/sell orders and match them … solely according to price and time.” — [OFAC filing](https://ofac.treasury.gov/system/files/2023-11/20231121_binance.pdf)

[^2]: “CME Globex Matching Algorithms” operate within dedicated matching systems, with algorithms like FIFO, pro-rata, etc. — [CME documentation](https://www.cmegroup.com/education/matching-algorithm-overview.html)

[^3]: Binance Academy: “Matching engines … follow predefined rules … e.g., FIFO.” — [Binance Academy](https://www.binance.com/en/square/post/5608556717810)

[^4]: Devexperts: “Order matching engine … matches buy and sell orders … using price‐time priority.” — [Devexperts Blog](https://devexperts.com/blog/order-matching-engine-everything-you-need-to-know/)

[^5]: “How Order Matching Engines Work…” — [Krayon Digital](https://www.krayondigital.com/blog/how-order-matching-engines-work-in-crypto-exchanges)

[^6]: “Using raw market data feeds for analytics and trading, separate from execution logic.” — [Databento](https://databento.com/pcaps)


---



## 🧠 Final Decision

We will adopt **Option 1** — to query the market price only once at the **moment of order creation** and store it in the `marketPrice` field.

This aligns with the principles of high-performance trading systems and supports future enhancements without coupling the core execution path to real-time price volatility.

## 🔮 Future Considerations

- Introduce streaming market data feeds for dashboards or analytics
- Add support for derived metrics like slippage and average execution price
- Consider dynamic pricing models for advanced contract types

## 📌 Notes

- This decision supports the milestone `v1.4.0-pricing`
- Referenced in the release documentation and main README milestone timeline