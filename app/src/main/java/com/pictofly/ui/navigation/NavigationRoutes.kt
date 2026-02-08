// app/src/main/java/com/pictofly/ui/navigation/NavigationRoutes.kt
package com.pictofly.ui.navigation

object NavigationRoutes {
    const val SPLASH = "splash"
    const val CONSENT = "consent"
    const val SOUND_CONFIG = "sound_config"
    const val SOUND_INPUT = "sound_input"
    const val PHYSIO_CONFIG = "physio_config"
    const val PATIENT_SELECTION = "patient_selection"
    const val JOYSTICK_TEST = "joystick_test"
    const val MAIN_INTERFACE = "main_interface"
    const val CONFIG_MENU = "config_menu"
    const val SOUND_CONFIG_FROM_MENU = "sound_config_from_menu"
    const val CALIBRATION_CONFIG_FROM_MENU = "calibration_config_from_menu"
    const val JOYSTICK_CONFIG_FROM_MENU = "joystick_config_from_menu"
    const val CATEGORY_DETAIL = "category_detail"
    const val PICTOGRAM_SIZE = "pictogram_size"
    const val MANAGE_CONTENT = "manage_content"
    const val ADD_CATEGORY = "add_category"
    const val ADD_PICTOGRAM = "add_pictogram"  // For single pictogram?
    const val ADD_PICTOGRAMS = "add_pictograms"

    // ✅ AGREGADO: Ruta para gestión de pictogramas por categoría
    const val CATEGORY_PICTOGRAMS = "category_pictograms"
    // ✅ RUTAS PARA MODO DE COMUNICACIÓN
    const val COMMUNICATION_MODE_MENU = "communication_mode_menu"  // Pantalla de 3 opciones
    const val COMMUNICATION_CUSTOM_PICTOGRAMS = "communication/custom_pictograms"  // Pantalla existente (renombrada)
    const val COMMUNICATION_CHANGE_SUBJECT = "communication/change_subject"
    const val COMMUNICATION_CHANGE_VERB = "communication/change_verb"
}