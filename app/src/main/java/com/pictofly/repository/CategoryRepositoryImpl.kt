package com.pictofly.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.pictofly.data.model.Category
import com.pictofly.data.model.Pictogram
import com.pictofly.data.model.LocalCategory
import com.pictofly.data.model.LocalPictogram
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val localContentRepository: LocalContentRepository,
    private val userOverrideRepository: UserPictogramOverrideRepository,
    private val context: Context
) : CategoryRepository {

    private val defaultCategories = listOf(
        Category("Sujeto","https://res.cloudinary.com/dvxwkfujl/image/upload/v1771212905/persona_de_medio_cuerpo_vs7zoa.png"),
        Category("Verbo","https://res.cloudinary.com/dvxwkfujl/image/upload/v1771213128/cambiar_de_mano_job22n.png"),
        Category("Emociones","https://res.cloudinary.com/dvxwkfujl/image/upload/v1758657559/emocion_ihbnlp.png"),
        Category("Higiene","https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653762/higiene_csfv2y.png"),
        Category("Juegos","https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653768/juegos_i77v59.png"),
        Category("Comida","https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656127/espaguetis_pewg3a.png"),
        Category("Bebidas","https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656392/naranjada_tkddzc.png"),
        Category("Frutas","https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653783/frutas_wqd6dq.png"),
    )

    private val pictogramsByCategory = mapOf(
        "Sujeto" to listOf(
            Pictogram("Yo","https://res.cloudinary.com/dvxwkfujl/image/upload/v1771213408/yo_m7zjbz.png"),
            Pictogram("Tu","https://res.cloudinary.com/dvxwkfujl/image/upload/v1771213414/t%C3%BA_g9hlsj.png"),
            Pictogram("El","https://res.cloudinary.com/dvxwkfujl/image/upload/v1771213420/%C3%A9l_dedpef.png"),
            Pictogram("Ella","https://res.cloudinary.com/dvxwkfujl/image/upload/v1771213425/ella_afznjf.png"),
            Pictogram("Nosotros","https://res.cloudinary.com/dvxwkfujl/image/upload/v1771213430/nosotros_dciitd.png"),
            Pictogram("Ellos","https://res.cloudinary.com/dvxwkfujl/image/upload/v1771213435/ellos_ox4ava.png"),
        ),
        "Verbo" to listOf(
            Pictogram("Quiero","https://res.cloudinary.com/dvxwkfujl/image/upload/v1771213883/yo_quiero_jtzlvw.png"),
            Pictogram("Necesito","https://res.cloudinary.com/dvxwkfujl/image/upload/v1771213890/necesito_ayuda_u0ivz2.png"),
            Pictogram("Dame","https://res.cloudinary.com/dvxwkfujl/image/upload/v1771213896/dame_m%C3%A1s_inwnjm.png"),
            Pictogram("Puedo","https://res.cloudinary.com/dvxwkfujl/image/upload/v1771213901/puedo_kv2xcq.png"),
            Pictogram("Siento","https://res.cloudinary.com/dvxwkfujl/image/upload/v1771213910/lo_siento_x7ovmt.png"),
            Pictogram("Estoy","https://res.cloudinary.com/dvxwkfujl/image/upload/v1771213917/estoy_regular_jbuhse.png"),
        ),
        "Frutas" to listOf(
            Pictogram("Kiwi", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758655355/kiwi_wmpnq9.png"),
            Pictogram("Sandía", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758655353/sand%C3%ADa_rhijtz.png"),
            Pictogram("Lima", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758655346/lima_qj00tv.png"),
            Pictogram("Papaya", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758655340/papaya_melmt3.png"),
            Pictogram("Manzana", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758655338/manzana_wsccfj.png"),
            Pictogram("Mandarina", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758655338/mandarina_rtge8f.png"),
            Pictogram("Plátano", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653783/fruta_platanos_nc2ea5.png"),
            Pictogram("Naranja", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765305377/rodaja_de_naranja_gqjfm4.png"),
            Pictogram("Piña", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653780/fruta_pinia_sdh6qq.png"),
            Pictogram("Mango", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765305472/mango_haracn.png"),
            Pictogram("Durazno", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765305535/durazno_cy9pwb.png"),
            Pictogram("Pera", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653782/fruta_pera_ft2pcg.png"),
            Pictogram("Chirimoya", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758655335/chirimoya_vk2iuo.png"),
            Pictogram("Coco", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758655332/coco_dusk7v.png"),
        ),
        "Emociones" to listOf(
            Pictogram("Feliz", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656388/emocionado_yyugo0.png"),
            Pictogram("Aburrido", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656387/aburrimiento_ge9na7.png"),
            Pictogram("Enojado", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653776/emociones_enfado_nzrg6w.png"),
            Pictogram("Triste", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653779/emociones_verguenza_argd9n.png"),
            Pictogram("Nervioso", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765306814/nervioso_wq7rgd.png"),
            Pictogram("Sorprendido", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656397/sorpresa_jilxoa.png"),
            Pictogram("Asustado", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656396/susto_z0j2nq.png"),
            Pictogram("Tranquilo", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765306858/tranquilo_lng4gi.png "),
            Pictogram("Satisfecho", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765306966/satisfecho_uydqb7.png"),
        ),
        "Higiene" to listOf(
            Pictogram("Peinar", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656619/peinar_ik33gh.png"),
            Pictogram("Secar", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656617/secar_rf2ms7.png"),
            Pictogram("Laven los pies", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656614/lavar_el_pie_bjir5y.png"),
            Pictogram("Limpien la nariz", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656613/sonar_c5ohti.png"),
            Pictogram("Lavar la cara ", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653762/higiene_lavarcara_hqifwo.png "),
            Pictogram("Cepillar dientes", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653762/higiene_lavardientes_fbflbh.png "),
            Pictogram("Bañarse", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1770932458/ducha_qzyyc1.png"),
            Pictogram("Hacer popo", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656611/limpiar_el_culo_r0f0rz.png"),
            Pictogram("Hacer pipi", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765307685/tener_ganas_de_hacer_pis_waccwv.png"),
            Pictogram("Papel higienico", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765307770/limpiar_el_pis_r0ut80.png"),
            Pictogram("Lavar manos  ", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653763/higiene_lavarmanos_zqwh1d.png "),
            Pictogram("Cambiar ropa ", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765307935/ropa_wqxezt.png "),
        ),
        "Juegos" to listOf(
            Pictogram("Futbol", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1770932324/golpear_el_bal%C3%B3n_e3evwp.png"),
            Pictogram("Carreritas", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765308274/carrera_de_coches_grxdwl.png"),
            Pictogram("Yo-yo", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653767/juego_yo_yo_ejqa1b.png"),
            Pictogram("Pelota terapeutica", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653766/juego_pelotaterapeutica_gji3aq.png"),
            Pictogram("Pasar pelota", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653765/juego_pasarlapelota_cncugr.png"),
            Pictogram("Burbujas", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765308335/burbujas_nnwnwy.png"),
            Pictogram("Pintar con pincel ", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765308398/pintar_tby7oq.png"),
            Pictogram("Pintar con crayones ", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765308494/pinturas_de_colores_cxiioi.png"),
            Pictogram("Pintar con colores ", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765308538/pinturas_de_colores_1_pxnebi.png "),
            Pictogram("Disfrazar", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765308646/corbata_de_payaso_wrbp2j.png"),
            Pictogram("Bailar", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765308719/baile_tlc7zc.png"),
        ),
        "Comida" to listOf(
            Pictogram("Pollo", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653772/proteina_pollo_wvovz1.png "),
            Pictogram("Carne ", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758653772/proteina_carne_kpurzc.png"),
            Pictogram("Pizza", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656043/trozo_de_pizza_ufnfio.png"),
            Pictogram("Sopa", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656123/sopa_de_pollo_agedoh.png "),
            Pictogram("Pesacado ", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656133/pescado_a_la_plancha_t8gezj.png "),
            Pictogram("Verduras", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1770934446/verduras_ehcyc8.png"),
            Pictogram("Pure", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656153/pur%C3%A9_bkvy1i.png"),
            Pictogram("Huevos revueltos", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656147/huevos_revueltos_psqp82.png"),
        ),
        "Bebidas" to listOf(
            Pictogram("Refresco", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758656393/limonada_qa6qrb.png"),
            Pictogram("Bebida caliente", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758655633/caliente_djukmg.png"),
            Pictogram("Bebida fria", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1758655627/fr%C3%ADo_z740dl.png"),
            Pictogram("Agua", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765308902/agua_nn0p89.png "),
            Pictogram("Jugo", "https://res.cloudinary.com/dvxwkfujl/image/upload/v1765309022/zumo_de_durazno_zm4egt.png"),
        )
    )

    override fun getAllCategories(): Flow<List<Category>> {
        return combine(
            localContentRepository.localCategoriesWithCount,
            flow { emit(userOverrideRepository) } //cada elem indi de la lista local cast
        ) { localCats: List<LocalCategory>, userOverrideRepo ->
            val filteredLocalCats = localCats.filterNot { localCat ->
                localCat.name == "Comunicación Personalizada" ||
                        localCat.name.startsWith("Extensión: ")
            }
            val convertedLocalCats = filteredLocalCats.mapNotNull { localCat ->
                try {
                    val imagePath = if (localCat.imagePath.isNotEmpty()) {
                        val file = if (localCat.imagePath.startsWith("/")) {
                            File(localCat.imagePath)
                        } else {
                            File(context.filesDir, localCat.imagePath)
                        }

                        if (file.exists()) {
                            file.absolutePath
                        } else {
                            Log.e("CategoryRepo", "Archivo no encontrado: ${localCat.imagePath}")
                            val fallbackFile = File(context.filesDir, "local_images/${File(localCat.imagePath).name}")
                            if (fallbackFile.exists()) {
                                fallbackFile.absolutePath
                            } else {
                                ""
                            }
                        }
                    } else {
                        ""
                    }
                    val fileUri = if (imagePath.isNotEmpty()) {
                        val file = File(imagePath) //objeto
                        if (file.exists()) {
                            try {
                                val uri = FileProvider.getUriForFile( //utl segura
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                uri.toString()
                            } catch (e: Exception) {
                                Log.e("CategoryRepo", "Error creando URI con FileProvider: ${e.message}")
                                null
                            }
                        } else {
                            null
                        }
                    } else {
                        null
                    }

                    Category(
                        name = localCat.name,
                        imageUrl = fileUri ?: "",
                        isLocal = true,
                        localImagePath = imagePath,
                        localFileUri = fileUri
                    )
                } catch (e: Exception) {
                    null
                }
            }   //categorias no ocultas
            val nonHiddenDefaultCategories = defaultCategories.filter { defaultCat ->
                !runBlocking {
                    userOverrideRepo.isCategoryDeleted(defaultCat.name)
                }
            }
            convertedLocalCats + nonHiddenDefaultCategories
        }
    }

    private fun createFileUri(imagePath: String): String {
        return try {
            if (imagePath.isEmpty()) {
                ""
            } else {
                val file = if (imagePath.startsWith("/")) {
                    File(imagePath)
                } else {
                    File(context.filesDir, imagePath)
                }

                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider", //convertimos porq Android NO permite compartir rutas directas
                        file
                    )
                    uri.toString()
                } else {
                    val fallbackFile = File(context.filesDir, "local_images/${File(imagePath).name}")
                    if (fallbackFile.exists()) {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            fallbackFile
                        )
                        uri.toString()
                    } else {
                        Log.e("CategoryRepo", "Archivo no encontrado: $imagePath")
                        ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CategoryRepo", "Error creando URI para archivo: ${e.message}")
            ""
        }
    }


    override fun getPictogramsByCategory(categoryName: String): Flow<List<Pictogram>> {
        return combine(
            localContentRepository.localCategoriesWithCount,
            localContentRepository.localPictograms,
            flow { emit(userOverrideRepository.getOverridesForCategory(categoryName)) }
        ) { localCategories, allLocalPictograms, userOverrides ->

            Log.d("CategoryRepo", "Buscando pictogramas para: $categoryName")
            Log.d("CategoryRepo", "Total pictogramas locales en sistema: ${allLocalPictograms.size}")
            Log.d("CategoryRepo", "Categorías locales disponibles:")
            localCategories.forEach { cat ->
                Log.d("CategoryRepo", "   - ${cat.name} (ID: ${cat.id})")
            }
            val defaultPictograms = if (!userOverrides.deleted) {
                pictogramsByCategory[categoryName] ?: emptyList()
            } else {
                emptyList()
            }
            Log.d("CategoryRepo", "Pictogramas predeterminados: ${defaultPictograms.size}")
            val visibleDefaults = defaultPictograms.filter { default ->
                !userOverrides.hiddenPictograms.contains(default.name)
            }.map { default ->
                Pictogram(
                    name = default.name,
                    imageUrl = default.imageUrl,
                    isLocal = false,
                    localImagePath = "",
                    isPredefined = true,
                    isVisible = true,
                    createdByUser = false
                )
            }
            Log.d("CategoryRepo", "Pictogramas predeterminados visibles: ${visibleDefaults.size}")
            val extensionCategoryName = "Extensión: $categoryName"
            val extensionCategory = localCategories.find { it.name == extensionCategoryName }

            Log.d("CategoryRepo", "Buscando extensión '$extensionCategoryName': ${if (extensionCategory != null) "ENCONTRADA con ID: ${extensionCategory.id}" else "NO ENCONTRADA"}")
            val extensionPictograms = if (extensionCategory != null) {
                allLocalPictograms
                    .filter { it.categoryId == extensionCategory.id }
                    .map { localPictogram ->
                        createPictogramFromLocal(localPictogram)
                    }
            } else {
                emptyList()
            }
            Log.d("CategoryRepo", "Pictogramas de extensión para '$categoryName': ${extensionPictograms.size}")
            val localCategory = localCategories.find { it.name == categoryName }
            val localPictograms = if (localCategory != null) {
                allLocalPictograms
                    .filter { it.categoryId == localCategory.id }
                    .map { localPictogram ->
                        createPictogramFromLocal(localPictogram)
                    }
            } else {
                emptyList()
            }
            Log.d("CategoryRepo", "Pictogramas locales para categoría local '$categoryName': ${localPictograms.size}")
            val userAddedPictograms = userOverrides.addedLocalPictograms.map { localPictogram ->
                createPictogramFromLocal(localPictogram)
            }
            Log.d("CategoryRepo", "Pictogramas añadidos por usuario (vía overrides): ${userAddedPictograms.size}")
            val additionalLocalPictograms = allLocalPictograms
                .filter { localPictogram ->
                    val pictogramCategory = localCategories.find { it.id == localPictogram.categoryId }
                    pictogramCategory?.name == categoryName ||
                            pictogramCategory?.name == extensionCategoryName
                }
                .map { localPictogram ->
                    createPictogramFromLocal(localPictogram)
                }
            Log.d("CategoryRepo", "Pictogramas adicionales: ${additionalLocalPictograms.size}")
            val allPictograms = (visibleDefaults +
                    extensionPictograms +
                    localPictograms +
                    userAddedPictograms +
                    additionalLocalPictograms)
            val uniquePictograms = mutableListOf<Pictogram>()
            val seenNames = mutableSetOf<String>()

            for (pictogram in allPictograms) {
                if (!seenNames.contains(pictogram.name)) { //guarda nombres que no aparecieron
                    seenNames.add(pictogram.name)
                    uniquePictograms.add(pictogram)
                } else {
                    Log.d("CategoryRepo", "Eliminando duplicado: ${pictogram.name}")
                }
            }

            Log.d("CategoryRepo", "Total pictogramas únicos para '$categoryName': ${uniquePictograms.size}")
            val sortedPictograms = uniquePictograms.sortedBy { !it.isPredefined }

            sortedPictograms
        }
    }

    //formato q puede usar la app
    private fun createPictogramFromLocal(localPictogram: LocalPictogram): Pictogram {
        val fileUri = createFileUri(localPictogram.imagePath)

        return Pictogram(
            name = localPictogram.name,
            imageUrl = fileUri,
            isLocal = true,
            localImagePath = localPictogram.imagePath,
            isPredefined = false,
            isVisible = true,
            createdByUser = true
        )
    }

    override fun getCategoryByName(name: String): Flow<Category?> {
        return getAllCategories()
            .map { categories: List<Category> ->
                categories.find { it.name == name }
            }
    }

    suspend fun isDefaultCategoryDeleted(categoryName: String): Boolean {
        return userOverrideRepository.isCategoryDeleted(categoryName)
    }

    suspend fun restoreDefaultCategory(categoryName: String) {
        userOverrideRepository.restoreCategory(categoryName)
    }
}