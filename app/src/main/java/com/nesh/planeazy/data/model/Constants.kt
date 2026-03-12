package com.nesh.planeazy.data.model

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

    // Professional Main Categories
    val EXPENSE_CATEGORIES = listOf(
        "Food & Dining",
        "Transport",
        "Housing",
        "Utilities",
        "Shopping",
        "Health & Wellness",
        "Education",
        "Entertainment",
        "Financial",
        "Personal Care",
        "Gifts & Donations",
        "Business",
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

    // Professional Hierarchical Category Mapping
    // "General" is implicitly the default if no sub-category is selected
    val SUB_CATEGORIES = mapOf(
        "Food & Dining" to listOf("Groceries", "Restaurants", "Fast Food", "Coffee & Snacks"),
        "Transport" to listOf("Fuel", "Public Transport", "Vehicle Maintenance", "Vehicle Insurance", "Parking", "Taxi/Ride-share"),
        "Housing" to listOf("Rent", "Mortgage", "Maintenance & Repairs", "Furniture", "Home Insurance"),
        "Utilities" to listOf("Electricity", "Water", "Gas", "Internet", "Phone / Mobile Data", "Trash & Sewer", "Security"),
        "Shopping" to listOf("Clothing", "Electronics", "Home Decor", "Beauty & Personal"),
        "Health & Wellness" to listOf("Doctor / Clinic", "Pharmacy / Meds", "Health Insurance", "Gym & Fitness", "Dental"),
        "Education" to listOf("Tuition / School Fees", "Online Courses", "Books & Supplies"),
        "Entertainment" to listOf("Movies & Events", "Streaming Services", "Hobbies", "Gaming"),
        "Financial" to listOf("Loan / Debt", "Investment", "Bank Fees", "Taxes", "Insurance"),
        "Personal Care" to listOf("Haircut", "Spa & Massage", "Skincare", "Cosmetics"),
        "Gifts & Donations" to listOf("Charity", "Family Support", "Holiday / Birthday Gifts"),
        "Business" to listOf("Marketing", "Office Supplies", "Work Travel", "Software / Tools")
    )

    // Map specific categories or sub-categories to their measurement units
    // Key can be either a Main Category or a Sub-Category
    val UNIT_MAPPING = mapOf(
        "Electricity" to "kWh",
        "Water" to "M³",
        "Gas" to "kg",
        "Fuel" to "Ltrs",
        "Internet" to "GB",
        "Phone / Mobile Data" to "GB"
    )
}
