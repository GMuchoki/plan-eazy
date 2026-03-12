package com.nesh.planeazy.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIcons {
    fun getIcon(category: String?): ImageVector {
        return when (category) {
            // Expense Categories
            "Food & Dining" -> Icons.Default.Restaurant
            "Transport" -> Icons.Default.DirectionsCar
            "Housing" -> Icons.Default.Home
            "Utilities" -> Icons.Default.Lightbulb
            "Shopping" -> Icons.Default.ShoppingBag
            "Health & Wellness" -> Icons.Default.MedicalServices
            "Education" -> Icons.Default.School
            "Entertainment" -> Icons.Default.Celebration
            "Financial" -> Icons.Default.AccountBalance
            "Personal Care" -> Icons.Default.Face
            "Gifts & Donations" -> Icons.Default.VolunteerActivism
            "Business" -> Icons.Default.BusinessCenter
            
            // Income Categories
            "Salary" -> Icons.Default.Payments
            "Freelance" -> Icons.Default.LaptopMac
            "Investment" -> Icons.AutoMirrored.Filled.TrendingUp
            "Gift" -> Icons.Default.Redeem
            
            // Sub Categories
            "Groceries" -> Icons.Default.ShoppingCart
            "Restaurants" -> Icons.Default.Restaurant
            "Fast Food" -> Icons.Default.Fastfood
            "Coffee & Snacks" -> Icons.Default.Coffee
            
            "Fuel" -> Icons.Default.LocalGasStation
            "Public Transport" -> Icons.Default.DirectionsBus
            "Vehicle Maintenance" -> Icons.Default.Build
            "Vehicle Insurance" -> Icons.Default.VerifiedUser
            "Parking" -> Icons.Default.LocalParking
            "Taxi/Ride-share" -> Icons.Default.LocalTaxi
            
            "Rent" -> Icons.Default.Apartment
            "Mortgage" -> Icons.Default.AccountBalanceWallet
            "Maintenance & Repairs" -> Icons.Default.Handyman
            "Furniture" -> Icons.Default.Chair
            "Home Insurance" -> Icons.Default.GppGood
            
            "Electricity" -> Icons.Default.ElectricBolt
            "Water" -> Icons.Default.WaterDrop
            "Gas" -> Icons.Default.PropaneTank
            "Internet" -> Icons.Default.Wifi
            "Phone / Mobile Data" -> Icons.Default.PhonelinkRing
            "Trash & Sewer" -> Icons.Default.DeleteSweep
            "Security" -> Icons.Default.Security
            
            "Clothing" -> Icons.Default.Checkroom
            "Electronics" -> Icons.Default.Devices
            "Home Decor" -> Icons.Default.HomeRepairService
            "Beauty & Personal" -> Icons.Default.Face
            
            "Doctor / Clinic" -> Icons.Default.LocalHospital
            "Pharmacy / Meds" -> Icons.Default.Medication
            "Health Insurance" -> Icons.Default.HealthAndSafety
            "Gym & Fitness" -> Icons.Default.FitnessCenter
            "Dental" -> Icons.Default.MedicalInformation
            
            "Tuition / School Fees" -> Icons.Default.School
            "Online Courses" -> Icons.Default.CastForEducation
            "Books & Supplies" -> Icons.Default.AutoStories
            
            "Movies & Events" -> Icons.Default.Movie
            "Streaming Services" -> Icons.Default.Tv
            "Hobbies" -> Icons.Default.Palette
            "Gaming" -> Icons.Default.SportsEsports
            
            "Loan / Debt" -> Icons.Default.MoneyOff
            "Bank Fees" -> Icons.Default.Atm
            "Taxes" -> Icons.AutoMirrored.Filled.ReceiptLong
            "Insurance" -> Icons.Default.Policy
            
            "Haircut" -> Icons.Default.ContentCut
            "Spa & Massage" -> Icons.Default.Spa
            "Skincare" -> Icons.Default.FaceRetouchingNatural
            "Cosmetics" -> Icons.Default.Brush
            
            "Charity" -> Icons.Default.Favorite
            "Family Support" -> Icons.Default.FamilyRestroom
            "Holiday / Birthday Gifts" -> Icons.Default.Cake
            
            "Marketing" -> Icons.Default.Campaign
            "Office Supplies" -> Icons.Default.Inventory
            "Work Travel" -> Icons.Default.Flight
            "Software / Tools" -> Icons.Default.SettingsSuggest

            // Goal Types
            "Emergency Fund" -> Icons.Default.HealthAndSafety
            "Vacation" -> Icons.Default.BeachAccess
            "Debt Payoff" -> Icons.Default.CreditScore
            "Business Capital" -> Icons.Default.Storefront
            "Birthdays/Events" -> Icons.Default.Event
            "Custom" -> Icons.Default.Stars

            else -> Icons.Default.Category
        }
    }
}
