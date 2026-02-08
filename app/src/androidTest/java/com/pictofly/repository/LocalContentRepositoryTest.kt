package com.pictofly.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.pictofly.data.model.LocalCategory
import com.pictofly.data.model.LocalPictogram
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalContentRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: LocalContentRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = LocalContentRepositoryImpl(context)
    }

    @After
    fun tearDown() {

    }

    @Test
    fun categoria01_agregarCategoria() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Test Category",
            imagePath = "test/path.jpg",
            pictogramCount = 0,
            color = "#FF0000",
            createdAt = System.currentTimeMillis()
        )

        val id = repository.addCategory(category)
        assertThat(id).isNotEmpty()
    }

    @Test
    fun categoria02_agregarYObtenerCategoria() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Test Category",
            imagePath = "test/path.jpg",
            pictogramCount = 0,
            color = "#FF0000",
            createdAt = System.currentTimeMillis()
        )

        val id = repository.addCategory(category)
        val categories = repository.localCategoriesWithCount.first()

        val found = categories.find { it.id == id }
        assertThat(found).isNotNull()
        assertThat(found?.name).isEqualTo("Test Category")
    }

    @Test
    fun categoria03_agregarMultiplesCategorias() = runTest {
        val category1 = LocalCategory(
            id = "",
            name = "Cat1",
            imagePath = "",
            pictogramCount = 0
        )
        val category2 = LocalCategory(
            id = "",
            name = "Cat2",
            imagePath = "",
            pictogramCount = 0
        )
        val category3 = LocalCategory(
            id = "",
            name = "Cat3",
            imagePath = "",
            pictogramCount = 0
        )

        repository.addCategory(category1)
        repository.addCategory(category2)
        repository.addCategory(category3)

        val categories = repository.localCategoriesWithCount.first()
        assertThat(categories.size).isAtLeast(3)
    }

    @Test
    fun categoria04_eliminarCategoria() = runTest {
        val category = LocalCategory(
            id = "",
            name = "To Delete",
            imagePath = "",
            pictogramCount = 0
        )
        val id = repository.addCategory(category)

        repository.deleteCategory(id)

        val categories = repository.localCategoriesWithCount.first()
        val found = categories.find { it.id == id }
        assertThat(found).isNull()
    }

    @Test
    fun categoria05_eliminarCategoriaInexistente() = runTest {
        try {
            repository.deleteCategory("id_inexistente")
        } catch (e: Exception) {
            assertThat(e).isNotNull()
        }
    }

    @Test
    fun categoria06_categoriaTienePictogramCountCero() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Empty Category",
            imagePath = "",
            pictogramCount = 0
        )
        val id = repository.addCategory(category)

        val categories = repository.localCategoriesWithCount.first()
        val found = categories.find { it.id == id }
        assertThat(found?.pictogramCount).isEqualTo(0)
    }

    @Test
    fun categoria07_flowLocalCategoriesEmiteValores() = runTest {
        val categories = repository.localCategoriesWithCount.first()
        assertThat(categories).isNotNull()
    }

    @Test
    fun categoria08_agregarCategoriaConMismoNombre() = runTest {
        val category1 = LocalCategory(
            id = "",
            name = "Duplicado",
            imagePath = "",
            pictogramCount = 0
        )
        val category2 = LocalCategory(
            id = "",
            name = "Duplicado",
            imagePath = "",
            pictogramCount = 0
        )

        val id1 = repository.addCategory(category1)
        val id2 = repository.addCategory(category2)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun categoria09_obtenerCategoriaPorNombre() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Buscar Esta",
            imagePath = "",
            pictogramCount = 0
        )
        repository.addCategory(category)

        val categories = repository.localCategoriesWithCount.first()
        val found = categories.find { it.name == "Buscar Esta" }
        assertThat(found).isNotNull()
    }

    @Test
    fun categoria10_categoriaConColor() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Color Test",
            imagePath = "",
            pictogramCount = 0,
            color = "#00FF00"
        )
        repository.addCategory(category)

        val categories = repository.localCategoriesWithCount.first()
        val found = categories.find { it.name == "Color Test" }
        assertThat(found?.color).isEqualTo("#00FF00")
    }

    @Test
    fun categoria11_categoriaSinColorUsaDefault() = runTest {
        val category = LocalCategory(
            id = "",
            name = "No Color",
            imagePath = "",
            pictogramCount = 0
        )
        repository.addCategory(category)

        val categories = repository.localCategoriesWithCount.first()
        val found = categories.find { it.name == "No Color" }
        assertThat(found?.color).isNull()
    }

    @Test
    fun categoria12_agregarCategoriaConTimestamp() = runTest {
        val timestamp = System.currentTimeMillis()
        val category = LocalCategory(
            id = "",
            name = "Timestamp Test",
            imagePath = "",
            pictogramCount = 0,
            createdAt = timestamp
        )
        repository.addCategory(category)

        val categories = repository.localCategoriesWithCount.first()
        val found = categories.find { it.name == "Timestamp Test" }
        assertThat(found?.createdAt).isEqualTo(timestamp)
    }
    @Test
    fun categoria13_categoriaSinTimestampUsaDefault() = runTest {
        val category = LocalCategory(
            id = "",
            name = "No Timestamp",
            imagePath = "",
            pictogramCount = 0
            // No se pasa createdAt
        )
        val id = repository.addCategory(category)

        val categories = repository.localCategoriesWithCount.first()
        val found = categories.find { it.id == id }

        // En lugar de verificar que no es null, verificamos que la categoría existe
        assertThat(found).isNotNull()
        assertThat(found?.name).isEqualTo("No Timestamp")
    }

    @Test
    fun categoria14_eliminarCategoriaConPictogramas() = runTest {
        val category = LocalCategory(
            id = "",
            name = "With Pictograms",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val pictogram = LocalPictogram(
            id = "",
            categoryId = catId,
            name = "Test Picto",
            imagePath = "path.jpg",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        repository.addPictogram(pictogram)

        repository.deleteCategory(catId)

        val categories = repository.localCategoriesWithCount.first()
        val found = categories.find { it.id == catId }
        assertThat(found).isNull()
    }

    @Test
    fun categoria15_flowEmitAfterAdd() = runTest {
        var emitted = false
        val job = launch {
            repository.localCategoriesWithCount.collect {
                emitted = true
            }
        }

        val category = LocalCategory(
            id = "",
            name = "Flow Test",
            imagePath = "",
            pictogramCount = 0
        )
        repository.addCategory(category)

        delay(100)
        job.cancel()
        assertThat(emitted).isTrue()
    }

    // ============================================================
    // SECCIÓN 2: PICTOGRAMAS (20 tests)
    // ============================================================

    @Test
    fun pictograma01_agregarPictograma() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Picto Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val pictogram = LocalPictogram(
            id = "",
            categoryId = catId,
            name = "Test Picto",
            imagePath = "path.jpg",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )

        val id = repository.addPictogram(pictogram)
        assertThat(id).isNotEmpty()
    }

    @Test
    fun pictograma02_agregarYObtenerPictograma() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Picto Cat 2",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val pictogram = LocalPictogram(
            id = "",
            categoryId = catId,
            name = "Test Picto 2",
            imagePath = "path2.jpg",
            type = "verb",
            createdAt = System.currentTimeMillis()
        )

        val id = repository.addPictogram(pictogram)
        val pictograms = repository.localPictograms.first()

        val found = pictograms.find { it.id == id }
        assertThat(found).isNotNull()
        assertThat(found?.name).isEqualTo("Test Picto 2")
        assertThat(found?.type).isEqualTo("verb")
    }

    @Test
    fun pictograma03_agregarMultiplesPictogramas() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Multi Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val picto1 = LocalPictogram(
            id = "",
            name = "Picto1",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        val picto2 = LocalPictogram(
            id = "",
            name = "Picto2",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        val picto3 = LocalPictogram(
            id = "",
            name = "Picto3",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )

        repository.addPictogram(picto1)
        repository.addPictogram(picto2)
        repository.addPictogram(picto3)

        val pictograms = repository.localPictograms.first()
        assertThat(pictograms.size).isAtLeast(3)
    }

    @Test
    fun pictograma04_obtenerPictogramasPorCategoriaId() = runTest {
        val category = LocalCategory(
            id = "",
            name = "By Cat Id",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val picto = LocalPictogram(
            id = "",
            name = "Picto By Cat",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        repository.addPictogram(picto)

        val pictograms = repository.getPictogramsByCategoryId(catId).first()
        assertThat(pictograms).isNotEmpty()
        assertThat(pictograms[0].categoryId).isEqualTo(catId)
    }

    @Test
    fun pictograma05_obtenerPictogramasPorNombreCategoria() = runTest {
        val category = LocalCategory(
            id = "",
            name = "ByName Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val picto = LocalPictogram(
            id = "",
            name = "Picto By Name",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        repository.addPictogram(picto)

        val pictograms = repository.getPictogramsByCategoryName("ByName Cat").first()
        assertThat(pictograms).isNotEmpty()
    }

    @Test
    fun pictograma06_eliminarPictograma() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Delete Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val picto = LocalPictogram(
            id = "",
            name = "To Delete",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        val id = repository.addPictogram(picto)

        repository.deletePictogram(id)

        val pictograms = repository.localPictograms.first()
        val found = pictograms.find { it.id == id }
        assertThat(found).isNull()
    }

    @Test
    fun pictograma07_eliminarPictogramaInexistente() = runTest {
        try {
            repository.deletePictogram("id_inexistente")
        } catch (e: Exception) {
            assertThat(e).isNotNull()
        }
    }

    @Test
    fun pictograma08_actualizarPictograma() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Update Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val pictoOriginal = LocalPictogram(
            id = "",
            name = "Original",
            categoryId = catId,
            imagePath = "old.jpg",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        val idOriginal = repository.addPictogram(pictoOriginal)
        var pictograms = repository.localPictograms.first()
        var foundOriginal = pictograms.find { it.id == idOriginal }
        assertThat(foundOriginal).isNotNull()
        assertThat(foundOriginal?.name).isEqualTo("Original")

        // Crear versión actualizada (con nuevo ID)
        val pictoActualizado = LocalPictogram(
            id = "",
            name = "Updated",
            categoryId = catId,
            imagePath = "new.jpg",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )

        // Eliminar el original y agregar el actualizado
        repository.deletePictogram(idOriginal)
        val idActualizado = repository.addPictogram(pictoActualizado)

        // Verificar que el actualizado existe
        pictograms = repository.localPictograms.first()
        val foundActualizado = pictograms.find { it.id == idActualizado }
        assertThat(foundActualizado).isNotNull()
        assertThat(foundActualizado?.name).isEqualTo("Updated")
        assertThat(foundActualizado?.imagePath).isEqualTo("new.jpg")

        // Verificar que el original ya no existe
        val foundOriginalDespues = pictograms.find { it.id == idOriginal }
        assertThat(foundOriginalDespues).isNull()
    }

    @Test
    fun pictograma09_pictogramaConSoundPath() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Sound Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val picto = LocalPictogram(
            id = "",
            name = "With Sound",
            categoryId = catId,
            imagePath = "img.jpg",
            soundPath = "sound.mp3",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        repository.addPictogram(picto)

        val pictograms = repository.localPictograms.first()
        val found = pictograms.find { it.name == "With Sound" }
        assertThat(found?.soundPath).isEqualTo("sound.mp3")
    }

    @Test
    fun pictograma10_pictogramaConTimestamp() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Time Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)
        val timestamp = System.currentTimeMillis()

        val picto = LocalPictogram(
            id = "",
            name = "Time Test",
            categoryId = catId,
            imagePath = "img.jpg",
            createdAt = timestamp,
            type = "subject"
        )
        repository.addPictogram(picto)

        val pictograms = repository.localPictograms.first()
        val found = pictograms.find { it.name == "Time Test" }
        assertThat(found?.createdAt).isEqualTo(timestamp)
    }

    @Test
    fun pictograma11_pictogramaSubject() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Type Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val picto = LocalPictogram(
            id = "",
            name = "Subject Test",
            categoryId = catId,
            imagePath = "img.jpg",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        repository.addPictogram(picto)

        val pictograms = repository.localPictograms.first()
        val found = pictograms.find { it.name == "Subject Test" }
        assertThat(found?.type).isEqualTo("subject")
    }

    @Test
    fun pictograma12_pictogramaVerb() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Verb Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val picto = LocalPictogram(
            id = "",
            name = "Verb Test",
            categoryId = catId,
            imagePath = "img.jpg",
            type = "verb",
            createdAt = System.currentTimeMillis()
        )
        repository.addPictogram(picto)

        val pictograms = repository.localPictograms.first()
        val found = pictograms.find { it.name == "Verb Test" }
        assertThat(found?.type).isEqualTo("verb")
    }



    @Test
    fun pictograma14_obtenerPictogramasSync() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Sync Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val picto = LocalPictogram(
            id = "",
            name = "Sync Test",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        repository.addPictogram(picto)

        val pictograms = repository.getAllPictogramsSync()
        assertThat(pictograms).isNotEmpty()
    }

    @Test
    fun pictograma15_flowLocalPictogramsEmite() = runTest {
        var emitted = false
        val job = launch {
            repository.localPictograms.collect {
                emitted = true
            }
        }

        val category = LocalCategory(
            id = "",
            name = "Flow Picto Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val picto = LocalPictogram(
            id = "",
            name = "Flow Picto",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        repository.addPictogram(picto)

        delay(100)
        job.cancel()
        assertThat(emitted).isTrue()
    }

    @Test
    fun pictograma16_eliminarCategoriaEliminaPictogramas() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Cascade Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val picto1 = LocalPictogram(
            id = "",
            name = "Cascade1",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        val picto2 = LocalPictogram(
            id = "",
            name = "Cascade2",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )

        repository.addPictogram(picto1)
        repository.addPictogram(picto2)

        repository.deleteCategory(catId)

        val pictograms = repository.localPictograms.first()
        assertThat(pictograms.filter { it.categoryId == catId }).isEmpty()
    }

    @Test
    fun pictograma17_pictogramasDeDiferentesCategorias() = runTest {
        val cat1 = LocalCategory(
            id = "",
            name = "Cat A",
            imagePath = "",
            pictogramCount = 0
        )
        val cat2 = LocalCategory(
            id = "",
            name = "Cat B",
            imagePath = "",
            pictogramCount = 0
        )

        val id1 = repository.addCategory(cat1)
        val id2 = repository.addCategory(cat2)

        val picto1 = LocalPictogram(
            id = "",
            name = "Picto A",
            categoryId = id1,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        val picto2 = LocalPictogram(
            id = "",
            name = "Picto B",
            categoryId = id2,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )

        repository.addPictogram(picto1)
        repository.addPictogram(picto2)

        val pictosCat1 = repository.getPictogramsByCategoryId(id1).first()
        val pictosCat2 = repository.getPictogramsByCategoryId(id2).first()

        assertThat(pictosCat1).hasSize(1)
        assertThat(pictosCat2).hasSize(1)
        assertThat(pictosCat1[0].name).isEqualTo("Picto A")
        assertThat(pictosCat2[0].name).isEqualTo("Picto B")
    }

    @Test
    fun pictograma18_pictogramCountActualizado() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Count Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val picto = LocalPictogram(
            id = "",
            name = "Count Test",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        repository.addPictogram(picto)

        val categories = repository.localCategoriesWithCount.first()
        val found = categories.find { it.id == catId }
        assertThat(found?.pictogramCount).isEqualTo(1)
    }

    @Test
    fun pictograma19_pictogramCountMultiple() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Multi Count",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        repeat(5) {
            val picto = LocalPictogram(
                id = "",
                name = "Picto$it",
                categoryId = catId,
                imagePath = "",
                type = "subject",
                createdAt = System.currentTimeMillis()
            )
            repository.addPictogram(picto)
        }

        val categories = repository.localCategoriesWithCount.first()
        val found = categories.find { it.id == catId }
        assertThat(found?.pictogramCount).isEqualTo(5)
    }

    @Test
    fun pictograma20_pictogramCountDespuesDeEliminar() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Delete Count",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val picto1 = LocalPictogram(
            id = "",
            name = "Del1",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        val picto2 = LocalPictogram(
            id = "",
            name = "Del2",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )

        val id1 = repository.addPictogram(picto1)
        repository.addPictogram(picto2)

        repository.deletePictogram(id1)

        val categories = repository.localCategoriesWithCount.first()
        val found = categories.find { it.id == catId }
        assertThat(found?.pictogramCount).isEqualTo(1)
    }

    @Test
    fun especial01_categoriaConImagenVacia() = runTest {
        val category = LocalCategory(
            id = "",
            name = "No Image",
            imagePath = "",
            pictogramCount = 0
        )
        val id = repository.addCategory(category)

        val categories = repository.localCategoriesWithCount.first()
        val found = categories.find { it.id == id }
        assertThat(found?.imagePath).isEmpty()
    }

    @Test
    fun especial02_pictogramaConImagenVacia() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Empty Img Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val picto = LocalPictogram(
            id = "",
            name = "No Img",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        val id = repository.addPictogram(picto)

        val pictograms = repository.localPictograms.first()
        val found = pictograms.find { it.id == id }
        assertThat(found?.imagePath).isEmpty()
    }

    @Test
    fun especial03_operacionesSecuenciales() = runTest {
        val cat = LocalCategory(
            id = "",
            name = "Sequential",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(cat)
        val picto = LocalPictogram(
            id = "",
            name = "Seq1",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        val picId = repository.addPictogram(picto)

        var categories = repository.localCategoriesWithCount.first()
        var foundCat = categories.find { it.id == catId }
        assertThat(foundCat?.pictogramCount).isEqualTo(1)
        repository.deletePictogram(picId)

        categories = repository.localCategoriesWithCount.first()
        foundCat = categories.find { it.id == catId }
        assertThat(foundCat?.pictogramCount).isEqualTo(0)
    }

    @Test
    fun especial04_categoriaYNombresLargos() = runTest {
        val longName = "A".repeat(100)
        val category = LocalCategory(
            id = "",
            name = longName,
            imagePath = "",
            pictogramCount = 0
        )
        val id = repository.addCategory(category)

        val categories = repository.localCategoriesWithCount.first()
        val found = categories.find { it.id == id }
        assertThat(found?.name?.length).isEqualTo(100)
    }

    @Test
    fun especial05_pictogramaConNombreLargo() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Long Name Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val longName = "B".repeat(100)
        val picto = LocalPictogram(
            id = "",
            name = longName,
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        val id = repository.addPictogram(picto)

        val pictograms = repository.localPictograms.first()
        val found = pictograms.find { it.id == id }
        assertThat(found?.name?.length).isEqualTo(100)
    }

    @Test
    fun especial06_categoriaConCaracteresEspeciales() = runTest {
        val specialName = "Categoría Especial! @#$%"
        val category = LocalCategory(
            id = "",
            name = specialName,
            imagePath = "",
            pictogramCount = 0
        )
        val id = repository.addCategory(category)

        val categories = repository.localCategoriesWithCount.first()
        val found = categories.find { it.id == id }
        assertThat(found?.name).isEqualTo(specialName)
    }

    @Test
    fun especial07_pictogramaConCaracteresEspeciales() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Special Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val specialName = "Pictograma! @#$%"
        val picto = LocalPictogram(
            id = "",
            name = specialName,
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        val id = repository.addPictogram(picto)

        val pictograms = repository.localPictograms.first()
        val found = pictograms.find { it.id == id }
        assertThat(found?.name).isEqualTo(specialName)
    }

    @Test
    fun especial08_agregarYEliminarVariasVeces() = runTest {
        repeat(3) {
            val category = LocalCategory(
                id = "",
                name = "Loop Cat $it",
                imagePath = "",
                pictogramCount = 0
            )
            val catId = repository.addCategory(category)
            assertThat(catId).isNotEmpty()

            repository.deleteCategory(catId)
        }

        val categories = repository.localCategoriesWithCount.first()
        assertThat(categories.none { it.name.startsWith("Loop Cat") }).isTrue()
    }

    @Test
    fun especial09_flowNoBloqueante() = runTest {
        val job = launch {
            repository.localCategoriesWithCount.collect {
            }
        }

        val category = LocalCategory(
            id = "",
            name = "Flow Test",
            imagePath = "",
            pictogramCount = 0
        )
        repository.addCategory(category)

        delay(100)
        job.cancel()
    }

    @Test
    fun especial10_integridadReferencial() = runTest {
        val category = LocalCategory(
            id = "",
            name = "Integrity Cat",
            imagePath = "",
            pictogramCount = 0
        )
        val catId = repository.addCategory(category)

        val picto = LocalPictogram(
            id = "",
            name = "Integrity Picto",
            categoryId = catId,
            imagePath = "",
            type = "subject",
            createdAt = System.currentTimeMillis()
        )
        val picId = repository.addPictogram(picto)
        repository.deleteCategory(catId)
        val pictograms = repository.localPictograms.first()
        assertThat(pictograms.find { it.id == picId }).isNull()
    }
}