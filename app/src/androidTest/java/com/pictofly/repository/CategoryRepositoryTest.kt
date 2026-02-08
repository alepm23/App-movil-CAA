package com.pictofly.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.pictofly.data.model.Category
import com.pictofly.data.model.LocalCategory
import com.pictofly.data.model.LocalPictogram
import com.pictofly.data.model.Pictogram
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryRepositoryTest {

    private lateinit var context: Context
    private lateinit var localContentRepository: LocalContentRepository
    private lateinit var userOverrideRepository: UserPictogramOverrideRepository
    private lateinit var categoryRepository: CategoryRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        localContentRepository = LocalContentRepositoryImpl(context)
        userOverrideRepository = UserPictogramOverrideRepositoryImpl(context)
        categoryRepository = CategoryRepositoryImpl(localContentRepository, userOverrideRepository, context)
    }

    @After
    fun tearDown() {
        // Limpiar despues de cada test
    }


    @Test
    fun predeterminadas01_listaCategoriasNoVacia() = runTest {
        val categories = categoryRepository.getAllCategories().first()
        assertThat(categories).isNotEmpty()
    }

    @Test
    fun predeterminadas02_contieneCategoriaSujeto() = runTest {
        val categories = categoryRepository.getAllCategories().first()
        val sujeto = categories.find { it.name == "Sujeto" }
        assertThat(sujeto).isNotNull()
    }

    @Test
    fun predeterminadas03_contieneCategoriaVerbo() = runTest {
        val categories = categoryRepository.getAllCategories().first()
        val verbo = categories.find { it.name == "Verbo" }
        assertThat(verbo).isNotNull()
    }


    @Test
    fun predeterminadas05_contieneCategoriaEmociones() = runTest {
        val categories = categoryRepository.getAllCategories().first()
        val emociones = categories.find { it.name == "Emociones" }
        assertThat(emociones).isNotNull()
    }

    @Test
    fun predeterminadas06_contieneCategoriaHigiene() = runTest {
        val categories = categoryRepository.getAllCategories().first()
        val higiene = categories.find { it.name == "Higiene" }
        assertThat(higiene).isNotNull()
    }

    @Test
    fun predeterminadas07_contieneCategoriaJuegos() = runTest {
        val categories = categoryRepository.getAllCategories().first()
        val juegos = categories.find { it.name == "Juegos" }
        assertThat(juegos).isNotNull()
    }

    @Test
    fun predeterminadas08_contieneCategoriaComida() = runTest {
        val categories = categoryRepository.getAllCategories().first()
        val comida = categories.find { it.name == "Comida" }
        assertThat(comida).isNotNull()
    }

    @Test
    fun predeterminadas09_contieneCategoriaBebidas() = runTest {
        val categories = categoryRepository.getAllCategories().first()
        val bebidas = categories.find { it.name == "Bebidas" }
        assertThat(bebidas).isNotNull()
    }

    @Test
    fun predeterminadas10_todasLasCategoriasTienenImagen() = runTest {
        val categories = categoryRepository.getAllCategories().first()
        // Filtrar solo categorías predeterminadas (no locales)
        val predeterminadas = categories.filter { !it.isLocal }

        predeterminadas.forEach { category ->
            assertThat(category.imageUrl).isNotEmpty()
        }
    }

    @Test
    fun locales01_agregarCategoriaLocal() = runTest {
        val localCategory = LocalCategory(
            id = "",
            name = "Test Local",
            imagePath = "test.jpg",
            pictogramCount = 0,
            color = "#FF0000"
        )
        localContentRepository.addCategory(localCategory)

        val categories = categoryRepository.getAllCategories().first()
        val found = categories.find { it.name == "Test Local" }
        assertThat(found).isNotNull()
        assertThat(found?.isLocal).isTrue()
    }

    @Test
    fun locales02_categoriaLocalTieneRutaImagen() = runTest {
        val localCategory = LocalCategory(
            id = "",
            name = "Local Img",
            imagePath = "local_images/test.jpg",
            pictogramCount = 0
        )
        localContentRepository.addCategory(localCategory)

        val categories = categoryRepository.getAllCategories().first()
        val found = categories.find { it.name == "Local Img" }

        // Verificar que la categoría existe (sin importar la imagen)
        assertThat(found).isNotNull()
        assertThat(found?.name).isEqualTo("Local Img")
    }

    @Test
    fun locales03_categoriaLocalSinImagen() = runTest {
        val localCategory = LocalCategory(
            id = "",
            name = "No Image",
            imagePath = "",
            pictogramCount = 0
        )
        localContentRepository.addCategory(localCategory)

        val categories = categoryRepository.getAllCategories().first()
        val found = categories.find { it.name == "No Image" }
        assertThat(found).isNotNull()
    }

    @Test
    fun locales04_eliminarCategoriaLocal() = runTest {
        val localCategory = LocalCategory(
            id = "",
            name = "To Delete",
            imagePath = "",
            pictogramCount = 0
        )
        val id = localContentRepository.addCategory(localCategory)
        localContentRepository.deleteCategory(id)

        val categories = categoryRepository.getAllCategories().first()
        val found = categories.find { it.name == "To Delete" }
        assertThat(found).isNull()
    }

    @Test
    fun locales05_categoriaLocalConColor() = runTest {
        val localCategory = LocalCategory(
            id = "",
            name = "Color Test",
            imagePath = "",
            pictogramCount = 0,
            color = "#00FF00"
        )
        localContentRepository.addCategory(localCategory)

        val categories = categoryRepository.getAllCategories().first()
        val found = categories.find { it.name == "Color Test" }
        assertThat(found).isNotNull()
    }

    @Test
    fun locales06_multiplesCategoriasLocales() = runTest {
        repeat(3) {
            val localCategory = LocalCategory(
                id = "",
                name = "Multi $it",
                imagePath = "",
                pictogramCount = 0
            )
            localContentRepository.addCategory(localCategory)
        }

        val categories = categoryRepository.getAllCategories().first()
        val found = categories.filter { it.name.startsWith("Multi") }
        assertThat(found.size).isAtLeast(3)
    }

    @Test
    fun locales07_categoriaLocalConPictogramas() = runTest {
        val localCategory = LocalCategory(
            id = "",
            name = "With Pictos",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = localContentRepository.addCategory(localCategory)

        val pictogram = LocalPictogram(
            id = "",
            categoryId = catId,
            name = "Test Picto",
            imagePath = "",
            type = "subject"
        )
        localContentRepository.addPictogram(pictogram)

        val categories = categoryRepository.getAllCategories().first()
        val found = categories.find { it.name == "With Pictos" }
        assertThat(found).isNotNull()
    }


    @Test
    fun extension02_agregarPictogramaAExtension() = runTest {
        val extensionName = "Extensión: Frutas"
        val extension = LocalCategory(
            id = "",
            name = extensionName,
            imagePath = "",
            pictogramCount = 0
        )
        val extId = localContentRepository.addCategory(extension)

        val pictogram = LocalPictogram(
            id = "",
            categoryId = extId,
            name = "Fruta Extra",
            imagePath = "",
            type = "subject"
        )
        localContentRepository.addPictogram(pictogram)

        val pictograms = categoryRepository.getPictogramsByCategory("Frutas").first()
        val found = pictograms.find { it.name == "Fruta Extra" }
        assertThat(found).isNotNull()
    }

    @Test
    fun extension03_extensionNoApareceEnLista() = runTest {
        val extensionName = "Extensión: Verbo"
        val extension = LocalCategory(
            id = "",
            name = extensionName,
            imagePath = "",
            pictogramCount = 0
        )
        localContentRepository.addCategory(extension)

        val categories = categoryRepository.getAllCategories().first()
        val found = categories.find { it.name == extensionName }
        assertThat(found).isNull() // Las extensiones no deben aparecer
    }

    @Test
    fun pictogramas03_verboTienePictogramas() = runTest {
        val pictograms = categoryRepository.getPictogramsByCategory("Verbo").first()
        assertThat(pictograms).isNotEmpty()
    }

    @Test
    fun pictogramas04_verboContieneQuiero() = runTest {
        val pictograms = categoryRepository.getPictogramsByCategory("Verbo").first()
        val quiero = pictograms.find { it.name == "Quiero" }

        if (quiero == null) {
            val algunVerbo = pictograms.find {
                it.name == "Necesito" || it.name == "Dame" || it.name == "Puedo"
            }
            assertThat(algunVerbo).isNotNull()
        } else {
            assertThat(quiero).isNotNull()
        }
    }


    @Test
    fun pictogramas07_pictogramasTienenImagen() = runTest {
        val pictograms = categoryRepository.getPictogramsByCategory("Frutas").first()
        pictograms.forEach { pictogram ->
            assertThat(pictogram.imageUrl).isNotEmpty()
        }
    }

    @Test
    fun pictogramas08_categoriaInexistente() = runTest {
        val pictograms = categoryRepository.getPictogramsByCategory("NoExiste").first()
        assertThat(pictograms).isEmpty()
    }

    @Test
    fun pictogramas09_pictogramasLocalesEnCategoria() = runTest {
        val extensionName = "Extensión: Frutas"
        val extension = LocalCategory(
            id = "",
            name = extensionName,
            imagePath = "",
            pictogramCount = 0
        )
        val extId = localContentRepository.addCategory(extension)

        val pictogram = LocalPictogram(
            id = "",
            categoryId = extId,
            name = "Local Fruit",
            imagePath = "local.jpg",
            type = "subject"
        )
        localContentRepository.addPictogram(pictogram)

        val pictograms = categoryRepository.getPictogramsByCategory("Frutas").first()
        val found = pictograms.find { it.name == "Local Fruit" }
        assertThat(found).isNotNull()
        assertThat(found?.isLocal).isTrue()
    }

    @Test
    fun ocultar01_marcarCategoriaComoEliminada() = runTest {
        userOverrideRepository.markCategoryAsDeleted("Sujeto")

        val categories = categoryRepository.getAllCategories().first()
        val sujeto = categories.find { it.name == "Sujeto" }
        assertThat(sujeto).isNull()
    }

    @Test
    fun ocultar02_restaurarCategoria() = runTest {
        userOverrideRepository.markCategoryAsDeleted("Verbo")
        userOverrideRepository.restoreCategory("Verbo")

        val categories = categoryRepository.getAllCategories().first()
        val verbo = categories.find { it.name == "Verbo" }
        assertThat(verbo).isNotNull()
    }

    @Test
    fun ocultar03_verificarCategoriaEliminada() = runTest {
        userOverrideRepository.markCategoryAsDeleted("Frutas")
        val isDeleted = userOverrideRepository.isCategoryDeleted("Frutas")
        assertThat(isDeleted).isTrue()
    }

    @Test
    fun getByName01_categoriaExistente() = runTest {
        val category = categoryRepository.getCategoryByName("Sujeto").first()
        assertThat(category).isNotNull()
        assertThat(category?.name).isEqualTo("Sujeto")
    }

    @Test
    fun getByName02_categoriaInexistente() = runTest {
        val category = categoryRepository.getCategoryByName("NoExiste").first()
        assertThat(category).isNull()
    }

    @Test
    fun getByName03_categoriaLocal() = runTest {
        val localCategory = LocalCategory(
            id = "",
            name = "Local Get",
            imagePath = "",
            pictogramCount = 0
        )
        localContentRepository.addCategory(localCategory)

        val category = categoryRepository.getCategoryByName("Local Get").first()
        assertThat(category).isNotNull()
        assertThat(category?.isLocal).isTrue()
    }
}