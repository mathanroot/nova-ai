package com.example.nova.nlu

data class IntentResult(
    val success: Boolean,
    val spokenResponse: String,
    val requiresFollowUp: Boolean = false,
    val followUpPrompt: String? = null,
    val disambiguationOptions: List<String>? = null,
    val extraData: Any? = null
) {
    companion object {
        fun success(response: String, requiresFollowUp: Boolean = false) =
            IntentResult(success = true, spokenResponse = response, requiresFollowUp = requiresFollowUp)

        fun failure(response: String) =
            IntentResult(success = false, spokenResponse = response, requiresFollowUp = false)

        fun clarification(prompt: String, options: List<String>) =
            IntentResult(
                success = true,
                spokenResponse = prompt,
                requiresFollowUp = true,
                followUpPrompt = prompt,
                disambiguationOptions = options
            )
    }
}
