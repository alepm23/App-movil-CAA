package com.pictofly.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.pictofly.data.model.LocalPictogram
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPictogramOverrideRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: UserPictogramOverrideRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = UserPictogramOverrideRepositoryImpl(context)
    }

    @After
    fun tearDown() {
    }

    @Test
    fun ocultar01_ocultarPictograma() = runTest {
        repository.hidePredefinedPictogram("Sujeto", "Yo")

        val overrides = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides.hiddenPictograms).contains("Yo")
    }

    @Test
    fun ocultar04_ocultarEnDiferentesCategorias() = runTest {
        repository.hidePredefinedPictogram("Sujeto", "Yo")
        repository.hidePredefinedPictogram("Verbo", "Quiero")

        val sujeto = repository.getOverridesForCategory("Sujeto")
        val verbo = repository.getOverridesForCategory("Verbo")

        assertThat(sujeto.hiddenPictograms).contains("Yo")
        assertThat(verbo.hiddenPictograms).contains("Quiero")
    }

    @Test
    fun ocultar05_mostrarPictogramaOculto() = runTest {
        repository.hidePredefinedPictogram("Sujeto", "Yo")
        repository.showPredefinedPictogram("Sujeto", "Yo")

        val overrides = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides.hiddenPictograms).doesNotContain("Yo")
    }

    @Test
    fun ocultar06_mostrarPictogramaNoOculto() = runTest {
        repository.showPredefinedPictogram("Sujeto", "Yo")

        val overrides = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides.hiddenPictograms).doesNotContain("Yo")
    }

    @Test
    fun anadir03_eliminarPictogramaLocal() = runTest {
        val pictogram = LocalPictogram(id = "1", name = "To Delete", categoryId = "cat1", imagePath = "")

        repository.addLocalPictogramToPredefinedCategory("Sujeto", pictogram)
        repository.removeUserAddedPictogram("Sujeto", "To Delete")

        val overrides = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides.addedLocalPictograms).isEmpty()
    }

    @Test
    fun anadir04_eliminarPictogramaInexistente() = runTest {
        repository.removeUserAddedPictogram("Sujeto", "NoExiste")

        val overrides = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides.addedLocalPictograms).isEmpty()
    }

    @Test
    fun eliminar01_marcarCategoriaEliminada() = runTest {
        repository.markCategoryAsDeleted("Sujeto")

        val isDeleted = repository.isCategoryDeleted("Sujeto")
        assertThat(isDeleted).isTrue()
    }

    @Test
    fun eliminar02_restaurarCategoria() = runTest {
        repository.markCategoryAsDeleted("Sujeto")
        repository.restoreCategory("Sujeto")

        val isDeleted = repository.isCategoryDeleted("Sujeto")
        assertThat(isDeleted).isFalse()
    }

    @Test
    fun eliminar03_eliminarVariasCategorias() = runTest {
        repository.markCategoryAsDeleted("Sujeto")
        repository.markCategoryAsDeleted("Verbo")
        repository.markCategoryAsDeleted("Frutas")

        assertThat(repository.isCategoryDeleted("Sujeto")).isTrue()
        assertThat(repository.isCategoryDeleted("Verbo")).isTrue()
        assertThat(repository.isCategoryDeleted("Frutas")).isTrue()
    }

    @Test
    fun eliminar04_restaurarUnaDeVarias() = runTest {
        repository.markCategoryAsDeleted("Sujeto")
        repository.markCategoryAsDeleted("Verbo")
        repository.restoreCategory("Sujeto")

        assertThat(repository.isCategoryDeleted("Sujeto")).isFalse()
        assertThat(repository.isCategoryDeleted("Verbo")).isTrue()
    }

    @Test
    fun combinacion02_eliminarCategoriaConOverrides() = runTest {
        repository.hidePredefinedPictogram("Sujeto", "Yo")
        val pictogram = LocalPictogram(id = "1", name = "Nuevo", categoryId = "cat1", imagePath = "")
        repository.addLocalPictogramToPredefinedCategory("Sujeto", pictogram)
        repository.markCategoryAsDeleted("Sujeto")

        val overrides = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides.deleted).isTrue()
        assertThat(overrides.hiddenPictograms).contains("Yo")
        assertThat(overrides.addedLocalPictograms).hasSize(1)
    }


    @Test
    fun especial01_categoriaConCaracteresEspeciales() = runTest {
        repository.hidePredefinedPictogram("Categoría Especial! @#$%", "Picto")
        val overrides = repository.getOverridesForCategory("Categoría Especial! @#$%")
        assertThat(overrides.hiddenPictograms).contains("Picto")
    }

    @Test
    fun especial02_pictogramaConCaracteresEspeciales() = runTest {
        repository.hidePredefinedPictogram("Sujeto", "Picto! @#$%")
        val overrides = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides.hiddenPictograms).contains("Picto! @#$%")
    }

    @Test
    fun especial03_categoriaVacia() = runTest {
        val overrides = repository.getOverridesForCategory("")
        assertThat(overrides.hiddenPictograms).isEmpty()
    }

    @Test
    fun especial04_pictogramaVacio() = runTest {
        repository.hidePredefinedPictogram("Sujeto", "")
        val overrides = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides.hiddenPictograms).contains("")
    }

    @Test
    fun especial05_categoriaMuyLarga() = runTest {
        val nombreLargo = "A".repeat(1000)
        repository.hidePredefinedPictogram(nombreLargo, "Picto")
        val overrides = repository.getOverridesForCategory(nombreLargo)
        assertThat(overrides.hiddenPictograms).contains("Picto")
    }

    @Test
    fun especial06_pictogramaMuyLargo() = runTest {
        val nombreLargo = "B".repeat(1000)
        repository.hidePredefinedPictogram("Sujeto", nombreLargo)
        val overrides = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides.hiddenPictograms).contains(nombreLargo)
    }

    @Test
    fun especial10_marcarYRestaurarCategoriaMultiple() = runTest {
        repeat(5) {
            repository.markCategoryAsDeleted("Sujeto")
            repository.restoreCategory("Sujeto")
        }
        assertThat(repository.isCategoryDeleted("Sujeto")).isFalse()
    }

    @Test
    fun especial11_overridesPersisten() = runTest {
        repository.hidePredefinedPictogram("Sujeto", "Yo")
        val overrides1 = repository.getOverridesForCategory("Sujeto")
        val overrides2 = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides1.hiddenPictograms).isEqualTo(overrides2.hiddenPictograms)
    }

    @Test
    fun especial12_categoriaNumerica() = runTest {
        repository.hidePredefinedPictogram("12345", "Picto")
        val overrides = repository.getOverridesForCategory("12345")
        assertThat(overrides.hiddenPictograms).contains("Picto")
    }

    @Test
    fun especial13_pictogramaNumerico() = runTest {
        repository.hidePredefinedPictogram("Sujeto", "12345")
        val overrides = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides.hiddenPictograms).contains("12345")
    }

    @Test
    fun especial14_categoriaConEspacios() = runTest {
        repository.hidePredefinedPictogram("  Categoría con espacios  ", "Picto")
        val overrides = repository.getOverridesForCategory("  Categoría con espacios  ")
        assertThat(overrides.hiddenPictograms).contains("Picto")
    }

    @Test
    fun especial15_pictogramaConEspacios() = runTest {
        repository.hidePredefinedPictogram("Sujeto", "  Picto con espacios  ")
        val overrides = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides.hiddenPictograms).contains("  Picto con espacios  ")
    }

    @Test
    fun especial16_categoriaYaIgnorada() = runTest {
        repository.markCategoryAsDeleted("Sujeto")
        repository.hidePredefinedPictogram("Sujeto", "Yo")
        val overrides = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides.deleted).isTrue()
        assertThat(overrides.hiddenPictograms).contains("Yo")
    }


    @Test
    fun especial19_overridesCategoriaInexistente() = runTest {
        val overrides = repository.getOverridesForCategory("NoExiste")
        assertThat(overrides.hiddenPictograms).isEmpty()
    }

    @Test
    fun especial20_operacionesRapidas() = runTest {
        repeat(100) {
            repository.hidePredefinedPictogram("Sujeto", "Picto$it")
        }
        val overrides = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides.hiddenPictograms).hasSize(100)
    }

    @Test
    fun especial21_mismoPictogramaDiferentesCategorias() = runTest {
        repository.hidePredefinedPictogram("Sujeto", "Mismo")
        repository.hidePredefinedPictogram("Verbo", "Mismo")

        val sujeto = repository.getOverridesForCategory("Sujeto")
        val verbo = repository.getOverridesForCategory("Verbo")

        assertThat(sujeto.hiddenPictograms).contains("Mismo")
        assertThat(verbo.hiddenPictograms).contains("Mismo")
    }

    @Test
    fun especial22_mostrarSinOcultar() = runTest {
        repository.showPredefinedPictogram("Sujeto", "NoOculto")
        val overrides = repository.getOverridesForCategory("Sujeto")
        assertThat(overrides.hiddenPictograms).doesNotContain("NoOculto")
    }

    @Test
    fun especial23_restaurarSinEliminar() = runTest {
        repository.restoreCategory("Sujeto")
        assertThat(repository.isCategoryDeleted("Sujeto")).isFalse()
    } }