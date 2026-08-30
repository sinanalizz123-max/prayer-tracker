package com.praytracker.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class PlaceSuggestion(
    val name: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String
) {
    val fullDisplayName: String
        get() = if (region.isNotBlank() && region != name) "$name, $region, $country" else "$name, $country"

    val shortDisplayName: String
        get() = "$name, $country"
}

object LocationSearchHelper {

    // Curated instant-cache of 200+ major cities & towns across all regions worldwide
    val POPULAR_WORLD_CITIES = listOf(
        // India & South Asia
        PlaceSuggestion("Delhi", "Delhi", "India", 28.6139, 77.2090, "Asia/Kolkata"),
        PlaceSuggestion("Mumbai", "Maharashtra", "India", 19.0760, 72.8777, "Asia/Kolkata"),
        PlaceSuggestion("Bengaluru", "Karnataka", "India", 12.9716, 77.5946, "Asia/Kolkata"),
        PlaceSuggestion("Hyderabad", "Telangana", "India", 17.3850, 78.4867, "Asia/Kolkata"),
        PlaceSuggestion("Chennai", "Tamil Nadu", "India", 13.0827, 80.2707, "Asia/Kolkata"),
        PlaceSuggestion("Kolkata", "West Bengal", "India", 22.5726, 88.3639, "Asia/Kolkata"),
        PlaceSuggestion("Ahmedabad", "Gujarat", "India", 23.0225, 72.5714, "Asia/Kolkata"),
        PlaceSuggestion("Pune", "Maharashtra", "India", 18.5204, 73.8567, "Asia/Kolkata"),
        PlaceSuggestion("Jaipur", "Rajasthan", "India", 26.9124, 75.7873, "Asia/Kolkata"),
        PlaceSuggestion("Lucknow", "Uttar Pradesh", "India", 26.8467, 80.9462, "Asia/Kolkata"),
        PlaceSuggestion("Srinagar", "Jammu and Kashmir", "India", 34.0837, 74.7973, "Asia/Kolkata"),
        PlaceSuggestion("Kozhikode", "Kerala", "India", 11.2588, 75.7804, "Asia/Kolkata"),
        PlaceSuggestion("Kochi", "Kerala", "India", 9.9312, 76.2673, "Asia/Kolkata"),
        PlaceSuggestion("Patna", "Bihar", "India", 25.5941, 85.1376, "Asia/Kolkata"),
        PlaceSuggestion("Bhopal", "Madhya Pradesh", "India", 23.2599, 77.4126, "Asia/Kolkata"),
        PlaceSuggestion("Karachi", "Sindh", "Pakistan", 24.8607, 67.0011, "Asia/Karachi"),
        PlaceSuggestion("Lahore", "Punjab", "Pakistan", 31.5204, 74.3587, "Asia/Karachi"),
        PlaceSuggestion("Islamabad", "ICT", "Pakistan", 33.6844, 73.0479, "Asia/Karachi"),
        PlaceSuggestion("Rawalpindi", "Punjab", "Pakistan", 33.5651, 73.0169, "Asia/Karachi"),
        PlaceSuggestion("Faisalabad", "Punjab", "Pakistan", 31.4504, 73.1350, "Asia/Karachi"),
        PlaceSuggestion("Peshawar", "Khyber Pakhtunkhwa", "Pakistan", 34.0151, 71.5249, "Asia/Karachi"),
        PlaceSuggestion("Quetta", "Balochistan", "Pakistan", 30.1798, 66.9750, "Asia/Karachi"),
        PlaceSuggestion("Dhaka", "Dhaka", "Bangladesh", 23.8103, 90.4125, "Asia/Dhaka"),
        PlaceSuggestion("Chittagong", "Chittagong", "Bangladesh", 22.3569, 91.7832, "Asia/Dhaka"),
        PlaceSuggestion("Sylhet", "Sylhet", "Bangladesh", 24.8949, 91.8687, "Asia/Dhaka"),
        PlaceSuggestion("Colombo", "Western", "Sri Lanka", 6.9271, 79.8612, "Asia/Colombo"),
        PlaceSuggestion("Male", "Kaafu", "Maldives", 4.1755, 73.5093, "Indian/Maldives"),
        PlaceSuggestion("Kabul", "Kabul", "Afghanistan", 34.5553, 69.2075, "Asia/Kabul"),

        // Middle East & North Africa
        PlaceSuggestion("Makkah", "Makkah", "Saudi Arabia", 21.3891, 39.8579, "Asia/Riyadh"),
        PlaceSuggestion("Madinah", "Madinah", "Saudi Arabia", 24.5247, 39.5692, "Asia/Riyadh"),
        PlaceSuggestion("Riyadh", "Riyadh", "Saudi Arabia", 24.7136, 46.6753, "Asia/Riyadh"),
        PlaceSuggestion("Jeddah", "Makkah", "Saudi Arabia", 21.4858, 39.1925, "Asia/Riyadh"),
        PlaceSuggestion("Dammam", "Eastern Province", "Saudi Arabia", 26.4207, 50.0888, "Asia/Riyadh"),
        PlaceSuggestion("Taif", "Makkah", "Saudi Arabia", 21.2854, 40.4244, "Asia/Riyadh"),
        PlaceSuggestion("Tabuk", "Tabuk", "Saudi Arabia", 28.3835, 36.5662, "Asia/Riyadh"),
        PlaceSuggestion("Dubai", "Dubai", "United Arab Emirates", 25.2048, 55.2708, "Asia/Dubai"),
        PlaceSuggestion("Abu Dhabi", "Abu Dhabi", "United Arab Emirates", 24.4539, 54.3773, "Asia/Dubai"),
        PlaceSuggestion("Sharjah", "Sharjah", "United Arab Emirates", 25.3463, 55.4209, "Asia/Dubai"),
        PlaceSuggestion("Ajman", "Ajman", "United Arab Emirates", 25.4052, 55.5136, "Asia/Dubai"),
        PlaceSuggestion("Al Ain", "Abu Dhabi", "United Arab Emirates", 24.2075, 55.7447, "Asia/Dubai"),
        PlaceSuggestion("Doha", "Ad-Dawhah", "Qatar", 25.2854, 51.5310, "Asia/Qatar"),
        PlaceSuggestion("Kuwait City", "Al Asimah", "Kuwait", 29.3759, 47.9774, "Asia/Kuwait"),
        PlaceSuggestion("Manama", "Capital", "Bahrain", 26.2285, 50.5860, "Asia/Bahrain"),
        PlaceSuggestion("Muscat", "Muscat", "Oman", 23.5880, 58.3829, "Asia/Muscat"),
        PlaceSuggestion("Salalah", "Dhofar", "Oman", 17.0151, 54.0924, "Asia/Muscat"),
        PlaceSuggestion("Cairo", "Cairo", "Egypt", 30.0444, 31.2357, "Africa/Cairo"),
        PlaceSuggestion("Alexandria", "Alexandria", "Egypt", 31.2001, 29.9187, "Africa/Cairo"),
        PlaceSuggestion("Giza", "Giza", "Egypt", 30.0131, 31.2089, "Africa/Cairo"),
        PlaceSuggestion("Amman", "Amman", "Jordan", 31.9454, 35.9284, "Asia/Amman"),
        PlaceSuggestion("Jerusalem", "Jerusalem", "Palestine", 31.7683, 35.2137, "Asia/Jerusalem"),
        PlaceSuggestion("Gaza", "Gaza", "Palestine", 31.5017, 34.4668, "Asia/Gaza"),
        PlaceSuggestion("Beirut", "Beirut", "Lebanon", 33.8938, 35.5018, "Asia/Beirut"),
        PlaceSuggestion("Damascus", "Damascus", "Syria", 33.5138, 36.2765, "Asia/Damascus"),
        PlaceSuggestion("Baghdad", "Baghdad", "Iraq", 33.3152, 44.3661, "Asia/Baghdad"),
        PlaceSuggestion("Erbil", "Kurdistan", "Iraq", 36.1901, 43.9930, "Asia/Baghdad"),
        PlaceSuggestion("Basra", "Basra", "Iraq", 30.5081, 47.7835, "Asia/Baghdad"),
        PlaceSuggestion("Tehran", "Tehran", "Iran", 35.6892, 51.3890, "Asia/Tehran"),
        PlaceSuggestion("Mashhad", "Razavi Khorasan", "Iran", 36.2972, 59.6067, "Asia/Tehran"),
        PlaceSuggestion("Isfahan", "Isfahan", "Iran", 32.6546, 51.6680, "Asia/Tehran"),
        PlaceSuggestion("Casablanca", "Casablanca-Settat", "Morocco", 33.5731, -7.5898, "Africa/Casablanca"),
        PlaceSuggestion("Rabat", "Rabat-Sale-Kenitra", "Morocco", 34.0209, -6.8416, "Africa/Casablanca"),
        PlaceSuggestion("Marrakech", "Marrakech-Safi", "Morocco", 31.6295, -7.9811, "Africa/Casablanca"),
        PlaceSuggestion("Fes", "Fes-Meknes", "Morocco", 34.0181, -5.0078, "Africa/Casablanca"),
        PlaceSuggestion("Tangier", "Tanger-Tetouan-Al Hoceima", "Morocco", 35.7595, -5.8340, "Africa/Casablanca"),
        PlaceSuggestion("Algiers", "Algiers", "Algeria", 36.7538, 3.0588, "Africa/Algiers"),
        PlaceSuggestion("Oran", "Oran", "Algeria", 35.6987, -0.6349, "Africa/Algiers"),
        PlaceSuggestion("Tunis", "Tunis", "Tunisia", 36.8065, 10.1815, "Africa/Tunis"),
        PlaceSuggestion("Tripoli", "Tripoli", "Libya", 32.8872, 13.1913, "Africa/Tripoli"),
        PlaceSuggestion("Khartoum", "Khartoum", "Sudan", 15.5007, 32.5599, "Africa/Khartoum"),

        // Southeast & East Asia
        PlaceSuggestion("Jakarta", "Jakarta", "Indonesia", -6.2088, 106.8456, "Asia/Jakarta"),
        PlaceSuggestion("Surabaya", "East Java", "Indonesia", -7.2575, 112.7521, "Asia/Jakarta"),
        PlaceSuggestion("Bandung", "West Java", "Indonesia", -6.9175, 107.6191, "Asia/Jakarta"),
        PlaceSuggestion("Medan", "North Sumatra", "Indonesia", 3.5952, 98.6722, "Asia/Jakarta"),
        PlaceSuggestion("Makassar", "South Sulawesi", "Indonesia", -5.1477, 119.4327, "Asia/Makassar"),
        PlaceSuggestion("Kuala Lumpur", "Federal Territory", "Malaysia", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        PlaceSuggestion("George Town", "Penang", "Malaysia", 5.4141, 100.3288, "Asia/Kuala_Lumpur"),
        PlaceSuggestion("Johor Bahru", "Johor", "Malaysia", 1.4927, 103.7414, "Asia/Kuala_Lumpur"),
        PlaceSuggestion("Kota Kinabalu", "Sabah", "Malaysia", 5.9804, 116.0735, "Asia/Kuching"),
        PlaceSuggestion("Singapore", "Central", "Singapore", 1.3521, 103.8198, "Asia/Singapore"),
        PlaceSuggestion("Bandar Seri Begawan", "Brunei-Muara", "Brunei", 4.9031, 114.9398, "Asia/Brunei"),
        PlaceSuggestion("Bangkok", "Bangkok", "Thailand", 13.7563, 100.5018, "Asia/Bangkok"),
        PlaceSuggestion("Manila", "Metro Manila", "Philippines", 14.5995, 120.9842, "Asia/Manila"),
        PlaceSuggestion("Tokyo", "Tokyo", "Japan", 35.6762, 139.6503, "Asia/Tokyo"),
        PlaceSuggestion("Seoul", "Seoul", "South Korea", 37.5665, 126.9780, "Asia/Seoul"),
        PlaceSuggestion("Beijing", "Beijing", "China", 39.9042, 116.4074, "Asia/Shanghai"),
        PlaceSuggestion("Hong Kong", "Hong Kong", "Hong Kong", 22.3193, 114.1694, "Asia/Hong_Kong"),

        // Turkey & Central Asia
        PlaceSuggestion("Istanbul", "Istanbul", "Turkey", 41.0082, 28.9784, "Europe/Istanbul"),
        PlaceSuggestion("Ankara", "Ankara", "Turkey", 39.9334, 32.8597, "Europe/Istanbul"),
        PlaceSuggestion("Izmir", "Izmir", "Turkey", 38.4237, 27.1428, "Europe/Istanbul"),
        PlaceSuggestion("Bursa", "Bursa", "Turkey", 40.1885, 29.0610, "Europe/Istanbul"),
        PlaceSuggestion("Antalya", "Antalya", "Turkey", 36.8969, 30.7133, "Europe/Istanbul"),
        PlaceSuggestion("Konya", "Konya", "Turkey", 37.8746, 32.4932, "Europe/Istanbul"),
        PlaceSuggestion("Gaziantep", "Gaziantep", "Turkey", 37.0662, 37.3833, "Europe/Istanbul"),
        PlaceSuggestion("Tashkent", "Tashkent", "Uzbekistan", 41.2995, 69.2401, "Asia/Tashkent"),
        PlaceSuggestion("Samarkand", "Samarkand", "Uzbekistan", 39.6270, 66.9750, "Asia/Tashkent"),
        PlaceSuggestion("Baku", "Baku", "Azerbaijan", 40.4093, 49.8671, "Asia/Baku"),
        PlaceSuggestion("Almaty", "Almaty", "Kazakhstan", 43.2220, 76.8512, "Asia/Almaty"),
        PlaceSuggestion("Astana", "Astana", "Kazakhstan", 51.1694, 71.4491, "Asia/Almaty"),

        // Europe
        PlaceSuggestion("London", "England", "United Kingdom", 51.5074, -0.1278, "Europe/London"),
        PlaceSuggestion("Birmingham", "England", "United Kingdom", 52.4862, -1.8904, "Europe/London"),
        PlaceSuggestion("Manchester", "England", "United Kingdom", 53.4808, -2.2426, "Europe/London"),
        PlaceSuggestion("Leeds", "England", "United Kingdom", 53.8008, -1.5491, "Europe/London"),
        PlaceSuggestion("Glasgow", "Scotland", "United Kingdom", 55.8642, -4.2518, "Europe/London"),
        PlaceSuggestion("Paris", "Ile-de-France", "France", 48.8566, 2.3522, "Europe/Paris"),
        PlaceSuggestion("Marseille", "Provence-Alpes-Cote d'Azur", "France", 43.2965, 5.3698, "Europe/Paris"),
        PlaceSuggestion("Lyon", "Auvergne-Rhone-Alpes", "France", 45.7640, 4.8357, "Europe/Paris"),
        PlaceSuggestion("Berlin", "Berlin", "Germany", 52.5200, 13.4050, "Europe/Berlin"),
        PlaceSuggestion("Munich", "Bavaria", "Germany", 48.1351, 11.5820, "Europe/Berlin"),
        PlaceSuggestion("Frankfurt", "Hesse", "Germany", 50.1109, 8.6821, "Europe/Berlin"),
        PlaceSuggestion("Hamburg", "Hamburg", "Germany", 53.5511, 9.9937, "Europe/Berlin"),
        PlaceSuggestion("Cologne", "North Rhine-Westphalia", "Germany", 50.9375, 6.9603, "Europe/Berlin"),
        PlaceSuggestion("Amsterdam", "North Holland", "Netherlands", 52.3676, 4.9041, "Europe/Amsterdam"),
        PlaceSuggestion("Rotterdam", "South Holland", "Netherlands", 51.9244, 4.4777, "Europe/Amsterdam"),
        PlaceSuggestion("Brussels", "Brussels", "Belgium", 50.8503, 4.3517, "Europe/Brussels"),
        PlaceSuggestion("Vienna", "Vienna", "Austria", 48.2082, 16.3738, "Europe/Vienna"),
        PlaceSuggestion("Zurich", "Zurich", "Switzerland", 47.3769, 8.5417, "Europe/Zurich"),
        PlaceSuggestion("Geneva", "Geneva", "Switzerland", 46.2044, 6.1432, "Europe/Zurich"),
        PlaceSuggestion("Rome", "Lazio", "Italy", 41.9028, 12.4964, "Europe/Rome"),
        PlaceSuggestion("Milan", "Lombardy", "Italy", 45.4642, 9.1900, "Europe/Rome"),
        PlaceSuggestion("Madrid", "Madrid", "Spain", 40.4168, -3.7038, "Europe/Madrid"),
        PlaceSuggestion("Barcelona", "Catalonia", "Spain", 41.3879, 2.1699, "Europe/Madrid"),
        PlaceSuggestion("Stockholm", "Stockholm", "Sweden", 59.3293, 18.0686, "Europe/Stockholm"),
        PlaceSuggestion("Oslo", "Oslo", "Norway", 59.9139, 10.7522, "Europe/Oslo"),
        PlaceSuggestion("Copenhagen", "Capital Region", "Denmark", 55.6761, 12.5683, "Europe/Copenhagen"),
        PlaceSuggestion("Dublin", "Leinster", "Ireland", 53.3498, -6.2603, "Europe/Dublin"),
        PlaceSuggestion("Moscow", "Moscow", "Russia", 55.7558, 37.6173, "Europe/Moscow"),
        PlaceSuggestion("Kazan", "Tatarstan", "Russia", 55.7887, 49.1221, "Europe/Moscow"),

        // Americas
        PlaceSuggestion("New York", "New York", "United States", 40.7128, -74.0060, "America/New_York"),
        PlaceSuggestion("Los Angeles", "California", "United States", 34.0522, -118.2437, "America/Los_Angeles"),
        PlaceSuggestion("Chicago", "Illinois", "United States", 41.8781, -87.6298, "America/Chicago"),
        PlaceSuggestion("Houston", "Texas", "United States", 29.7604, -95.3698, "America/Chicago"),
        PlaceSuggestion("Dallas", "Texas", "United States", 32.7767, -96.7970, "America/Chicago"),
        PlaceSuggestion("San Francisco", "California", "United States", 37.7749, -122.4194, "America/Los_Angeles"),
        PlaceSuggestion("Seattle", "Washington", "United States", 47.6062, -122.3321, "America/Los_Angeles"),
        PlaceSuggestion("Philadelphia", "Pennsylvania", "United States", 39.9526, -75.1652, "America/New_York"),
        PlaceSuggestion("Washington, D.C.", "District of Columbia", "United States", 38.9072, -77.0369, "America/New_York"),
        PlaceSuggestion("Atlanta", "Georgia", "United States", 33.7490, -84.3880, "America/New_York"),
        PlaceSuggestion("Miami", "Florida", "United States", 25.7617, -80.1918, "America/New_York"),
        PlaceSuggestion("Detroit", "Michigan", "United States", 42.3314, -83.0458, "America/Detroit"),
        PlaceSuggestion("Dearborn", "Michigan", "United States", 42.3223, -83.1763, "America/Detroit"),
        PlaceSuggestion("Toronto", "Ontario", "Canada", 43.6532, -79.3832, "America/Toronto"),
        PlaceSuggestion("Montreal", "Quebec", "Canada", 45.5017, -73.5673, "America/Toronto"),
        PlaceSuggestion("Vancouver", "British Columbia", "Canada", 49.2827, -123.1207, "America/Vancouver"),
        PlaceSuggestion("Calgary", "Alberta", "Canada", 51.0447, -114.0719, "America/Edmonton"),
        PlaceSuggestion("Ottawa", "Ontario", "Canada", 45.4215, -75.6972, "America/Toronto"),
        PlaceSuggestion("Mexico City", "CDMX", "Mexico", 19.4326, -99.1332, "America/Mexico_City"),
        PlaceSuggestion("Sao Paulo", "Sao Paulo", "Brazil", -23.5505, -46.6333, "America/Sao_Paulo"),
        PlaceSuggestion("Buenos Aires", "Buenos Aires", "Argentina", -34.6037, -58.3816, "America/Argentina/Buenos_Aires"),

        // Africa & Oceania
        PlaceSuggestion("Johannesburg", "Gauteng", "South Africa", -26.2041, 28.0473, "Africa/Johannesburg"),
        PlaceSuggestion("Cape Town", "Western Cape", "South Africa", -33.9249, 18.4241, "Africa/Johannesburg"),
        PlaceSuggestion("Durban", "KwaZulu-Natal", "South Africa", -29.8587, 31.0218, "Africa/Johannesburg"),
        PlaceSuggestion("Nairobi", "Nairobi", "Kenya", -1.2921, 36.8219, "Africa/Nairobi"),
        PlaceSuggestion("Mombasa", "Mombasa", "Kenya", -4.0435, 39.6682, "Africa/Nairobi"),
        PlaceSuggestion("Lagos", "Lagos", "Nigeria", 6.5244, 3.3792, "Africa/Lagos"),
        PlaceSuggestion("Abuja", "FCT", "Nigeria", 9.0765, 7.3986, "Africa/Lagos"),
        PlaceSuggestion("Kano", "Kano", "Nigeria", 12.0022, 8.5920, "Africa/Lagos"),
        PlaceSuggestion("Dakar", "Dakar", "Senegal", 14.7167, -17.4677, "Africa/Dakar"),
        PlaceSuggestion("Addis Ababa", "Addis Ababa", "Ethiopia", 9.0300, 38.7400, "Africa/Addis_Ababa"),
        PlaceSuggestion("Sydney", "New South Wales", "Australia", -33.8688, 151.2093, "Australia/Sydney"),
        PlaceSuggestion("Melbourne", "Victoria", "Australia", -37.8136, 144.9631, "Australia/Melbourne"),
        PlaceSuggestion("Brisbane", "Queensland", "Australia", -27.4698, 153.0251, "Australia/Brisbane"),
        PlaceSuggestion("Perth", "Western Australia", "Australia", -31.9505, 115.8605, "Australia/Perth"),
        PlaceSuggestion("Adelaide", "South Australia", "Australia", -34.9285, 138.6007, "Australia/Adelaide"),
        PlaceSuggestion("Auckland", "Auckland", "New Zealand", -36.8485, 174.7633, "Pacific/Auckland")
    )

    suspend fun searchPlaces(context: Context, query: String): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext POPULAR_WORLD_CITIES.take(15)
        }

        val results = mutableListOf<PlaceSuggestion>()

        // 1. First search in our fast in-memory dataset
        val localMatches = POPULAR_WORLD_CITIES.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
            it.country.contains(trimmed, ignoreCase = true) ||
            it.region.contains(trimmed, ignoreCase = true)
        }
        results.addAll(localMatches)

        // 2. Search dynamically using Android Geocoder for any town/village/city worldwide
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var list: List<Address>? = null
                    try {
                        list = geocoder.getFromLocationName(trimmed, 8)
                    } catch (e: Exception) {
                        null
                    }
                    list
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(trimmed, 8)
                }

                if (!addresses.isNullOrEmpty()) {
                    for (addr in addresses) {
                        val cityName = addr.locality 
                            ?: addr.subAdminArea 
                            ?: addr.adminArea 
                            ?: addr.featureName 
                            ?: trimmed
                        val countryName = addr.countryName ?: ""
                        val regionName = addr.adminArea ?: ""
                        
                        // Prevent exact duplicate Lat/Lon entries
                        val isDuplicate = results.any {
                            Math.abs(it.latitude - addr.latitude) < 0.05 &&
                            Math.abs(it.longitude - addr.longitude) < 0.05
                        }

                        if (!isDuplicate) {
                            val tz = determineTimezone(addr.latitude, addr.longitude)
                            results.add(
                                PlaceSuggestion(
                                    name = cityName,
                                    region = regionName,
                                    country = countryName,
                                    latitude = addr.latitude,
                                    longitude = addr.longitude,
                                    timezoneId = tz
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Geocoder service may be busy or offline, fallback safely to local matches
        }

        return@withContext results.distinctBy { "${it.name}_${it.country}" }
    }

    private fun determineTimezone(lat: Double, lon: Double): String {
        // Approximate timezone from longitude offset or default to system
        return try {
            val offsetHours = Math.round(lon / 15.0).toInt()
            val available = java.util.TimeZone.getAvailableIDs(offsetHours * 3600 * 1000)
            if (available.isNotEmpty()) available[0] else java.util.TimeZone.getDefault().id
        } catch (e: Exception) {
            java.util.TimeZone.getDefault().id
        }
    }
}
