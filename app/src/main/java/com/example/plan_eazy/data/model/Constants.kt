package com.example.plan_eazy.data.model

object Constants {
    val CURRENCIES = listOf(
        "KES" to "Shilling",
        "USD" to "Dollar",
        "EUR" to "Euro",
        "GBP" to "Pound",
        "NGN" to "Naira",
        "ZAR" to "Rand",
        "UGX" to "Uganda Shilling",
        "TZS" to "Tanzania Shilling"
    )

    val PAYMENT_METHOD_TYPES = listOf(
        "Cash",
        "Mobile Money",
        "Bank",
        "Card",
        "Online Wallet",
        "Other"
    )

    val DEFAULT_GOAL_TYPES = listOf(
        "Emergency Fund",
        "Rent",
        "School Fees",
        "Vacation",
        "Debt Payoff",
        "Investment",
        "Business Capital",
        "Birthdays/Events",
        "Family Support",
        "Custom"
    )

    val EXPENSE_CATEGORIES = listOf(
        "Food & Groceries",
        "Transport",
        "Utilities",
        "Rent",
        "Shopping",
        "Entertainment",
        "Health",
        "Education",
        "Personal Care",
        "Other"
    )

    val INCOME_CATEGORIES = listOf(
        "Salary",
        "Business",
        "Freelance",
        "Investment",
        "Gift",
        "Other"
    )
}
