package com.pictofly.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class PersonaGramatical {
    YO, TU, EL, NOSOTROS, VOSOTROS, ELLOS
}

object MapeoSujetos {
    private val sujetos = mapOf(
        "yo" to PersonaGramatical.YO,
        "tú" to PersonaGramatical.TU,
        "tu" to PersonaGramatical.TU,
        "usted" to PersonaGramatical.EL,
        "él" to PersonaGramatical.EL,
        "el" to PersonaGramatical.EL,
        "ella" to PersonaGramatical.EL,
        "nosotros" to PersonaGramatical.NOSOTROS,
        "nosotras" to PersonaGramatical.NOSOTROS,
        "vosotros" to PersonaGramatical.VOSOTROS,
        "vosotras" to PersonaGramatical.VOSOTROS,
        "ellos" to PersonaGramatical.ELLOS,
        "ellas" to PersonaGramatical.ELLOS
    )

    fun obtenerPersona(palabra: String): PersonaGramatical? {
        return sujetos[palabra.lowercase().trim()]
    }
}

data class ResultadoCorreccion(
    val fraseOriginal: String,
    val fraseCorregida: String,
    val fueCorregido: Boolean,
    val verboOriginal: String? = null,
    val verboCorregido: String? = null,
    val predicadoOriginal: String? = null,
    val predicadoCorregido: String? = null
)

@Singleton
class AdaptiveGrammarEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val whitespaceRegex = Regex("\\s+")

    private val normalizacionVerbos = mapOf(
        "necesito" to "necesitar",
        "quiero" to "querer",
        "puedo" to "poder",
        "voy" to "ir",
        "tengo" to "tener",
        "juego" to "jugar",
        "estoy" to "estar",
        "soy" to "ser",
        "como" to "comer",
        "tomo" to "tomar"
    )

    private val irregulares = mapOf(
        "estar" to mapOf(
            PersonaGramatical.YO to "estoy", PersonaGramatical.TU to "estás",
            PersonaGramatical.EL to "está", PersonaGramatical.NOSOTROS to "estamos",
            PersonaGramatical.VOSOTROS to "estáis", PersonaGramatical.ELLOS to "están"
        ),
        "ser" to mapOf(
            PersonaGramatical.YO to "soy", PersonaGramatical.TU to "eres",
            PersonaGramatical.EL to "es", PersonaGramatical.NOSOTROS to "somos",
            PersonaGramatical.VOSOTROS to "sois", PersonaGramatical.ELLOS to "son"
        ),
        "ir" to mapOf(
            PersonaGramatical.YO to "voy", PersonaGramatical.TU to "vas",
            PersonaGramatical.EL to "va", PersonaGramatical.NOSOTROS to "vamos",
            PersonaGramatical.VOSOTROS to "vais", PersonaGramatical.ELLOS to "van"
        ),
        "tener" to mapOf(
            PersonaGramatical.YO to "tengo", PersonaGramatical.TU to "tienes",
            PersonaGramatical.EL to "tiene", PersonaGramatical.NOSOTROS to "tenemos",
            PersonaGramatical.VOSOTROS to "tenéis", PersonaGramatical.ELLOS to "tienen"
        ),
        "querer" to mapOf(
            PersonaGramatical.YO to "quiero", PersonaGramatical.TU to "quieres",
            PersonaGramatical.EL to "quiere", PersonaGramatical.NOSOTROS to "queremos",
            PersonaGramatical.VOSOTROS to "queréis", PersonaGramatical.ELLOS to "quieren"
        ),
        "necesitar" to mapOf(
            PersonaGramatical.YO to "necesito", PersonaGramatical.TU to "necesitas",
            PersonaGramatical.EL to "necesita", PersonaGramatical.NOSOTROS to "necesitamos",
            PersonaGramatical.VOSOTROS to "necesitáis", PersonaGramatical.ELLOS to "necesitan"
        ),
        "poder" to mapOf(
            PersonaGramatical.YO to "puedo", PersonaGramatical.TU to "puedes",
            PersonaGramatical.EL to "puede", PersonaGramatical.NOSOTROS to "podemos",
            PersonaGramatical.VOSOTROS to "podéis", PersonaGramatical.ELLOS to "pueden"
        ),
        "jugar" to mapOf(
            PersonaGramatical.YO to "juego", PersonaGramatical.TU to "juegas",
            PersonaGramatical.EL to "juega", PersonaGramatical.NOSOTROS to "jugamos",
            PersonaGramatical.VOSOTROS to "jugáis", PersonaGramatical.ELLOS to "juegan"
        ),
        "comer" to mapOf(
            PersonaGramatical.YO to "como", PersonaGramatical.TU to "comes",
            PersonaGramatical.EL to "come", PersonaGramatical.NOSOTROS to "comemos",
            PersonaGramatical.VOSOTROS to "coméis", PersonaGramatical.ELLOS to "comen"
        ),
        "tomar" to mapOf(
            PersonaGramatical.YO to "tomo", PersonaGramatical.TU to "tomas",
            PersonaGramatical.EL to "toma", PersonaGramatical.NOSOTROS to "tomamos",
            PersonaGramatical.VOSOTROS to "tomáis", PersonaGramatical.ELLOS to "toman"
        ),
        "sentir" to mapOf(
            PersonaGramatical.YO to "siento", PersonaGramatical.TU to "sientes",
            PersonaGramatical.EL to "siente", PersonaGramatical.NOSOTROS to "sentimos",
            PersonaGramatical.VOSOTROS to "sentís", PersonaGramatical.ELLOS to "sienten"
        )
    )

    private val pronombresReflexivos = mapOf(
        PersonaGramatical.YO to "me", PersonaGramatical.TU to "te",
        PersonaGramatical.EL to "se", PersonaGramatical.NOSOTROS to "nos",
        PersonaGramatical.VOSOTROS to "os", PersonaGramatical.ELLOS to "se"
    )

    fun corregirFrase(sujeto: String?, verbo: String?, predicado: String?): ResultadoCorreccion {
        val s = sujeto?.trim()
        val v = verbo?.trim()
        val p = predicado?.trim()

        if (s.isNullOrBlank() && v.isNullOrBlank()) {
            return ResultadoCorreccion(
                fraseOriginal = p ?: "",
                fraseCorregida = p ?: "",
                fueCorregido = false
            )
        }

        if (s.isNullOrBlank()) {
            val frase = listOfNotNull(v, p).joinToString(" ")
            return ResultadoCorreccion(frase, frase, false)
        }

        if (p.isNullOrBlank()) {
            val frase = listOfNotNull(s, v).joinToString(" ")
            return ResultadoCorreccion(frase, frase, false)
        }

        val persona = MapeoSujetos.obtenerPersona(s) ?: PersonaGramatical.YO
        val predicadoLimpio = p.lowercase()

        if (v.isNullOrBlank()) {
            return manejarSinVerbo(s, persona, predicadoLimpio, p, s + " " + p)
        }

        return manejarConVerbo(s, persona, v, predicadoLimpio, p, "$s $v $p")
    }

    private fun manejarSinVerbo(
        sujeto: String,
        persona: PersonaGramatical,
        predicadoLimpio: String,
        predicadoOriginal: String,
        fraseOriginal: String
    ): ResultadoCorreccion {
        val (verboBase, predicadoProcesado) = detectarIntencion(predicadoLimpio)

        // Detectar si es verbo reflexivo
        val esReflexivo = when (verboBase) {
            "sentir" -> true
            else -> verboBase.endsWith("se")
        }

        // Obtener verbo limpio para conjugar
        val verboParaConjugar = if (verboBase == "sentir") "sentir" else limpiarVerbo(verboBase)

        // Conjugar el verbo según la persona
        val verboConjugado = conjugar(verboParaConjugar, persona)

        // Pronombre reflexivo correcto
        val pronombreReflexivo = if (esReflexivo) "${pronombresReflexivos[persona]} " else ""

        // Adaptar adjetivos a plural si es necesario
        val predicadoAdaptado = if (persona in setOf(PersonaGramatical.NOSOTROS, PersonaGramatical.VOSOTROS, PersonaGramatical.ELLOS)) {
            adaptarAdjetivoAPlural(predicadoProcesado)
        } else {
            predicadoProcesado
        }

        val fraseFinal = "$sujeto $pronombreReflexivo$verboConjugado $predicadoAdaptado"
            .trim()
            .replace(whitespaceRegex, " ")

        return ResultadoCorreccion(
            fraseOriginal,
            fraseFinal,
            true,
            null,
            verboConjugado,
            predicadoOriginal,
            predicadoAdaptado
        )
    }

    private fun manejarConVerbo(
        sujeto: String,
        persona: PersonaGramatical,
        verboSeleccionado: String,
        predicadoLimpio: String,
        predicadoOriginal: String,
        fraseOriginal: String
    ): ResultadoCorreccion {
        val verboNormalizado = normalizacionVerbos[verboSeleccionado.lowercase().trim()] ?: verboSeleccionado
        val verboBase = limpiarVerbo(verboNormalizado)
        val verboConjugado = conjugar(verboBase, persona)

        val predicadoFinal = if (esInfinitivo(predicadoLimpio)) {
            predicadoLimpio
        } else {
            procesarPredicadoConVerbo(verboBase, predicadoLimpio, persona)
        }

        val esReflexivo = verboSeleccionado.lowercase().endsWith("se")
        val pronombre = if (esReflexivo) "${pronombresReflexivos[persona]} " else ""

        val fraseFinal = "$sujeto $pronombre$verboConjugado $predicadoFinal".trim().replace(whitespaceRegex, " ")

        return ResultadoCorreccion(fraseOriginal, fraseFinal, true, verboSeleccionado, verboConjugado, predicadoOriginal, predicadoFinal)
    }

    private fun conjugar(verbo: String, persona: PersonaGramatical): String {
        val v = verbo.lowercase().trim()
        irregulares[v]?.get(persona)?.let { return it }

        if (v.length <= 2) return v
        val terminacion = v.takeLast(2)
        val raiz = v.dropLast(2)

        return when (terminacion) {
            "ar" -> when (persona) {
                PersonaGramatical.YO -> "${raiz}o"
                PersonaGramatical.TU -> "${raiz}as"
                PersonaGramatical.EL -> "${raiz}a"
                PersonaGramatical.NOSOTROS -> "${raiz}amos"
                PersonaGramatical.VOSOTROS -> "${raiz}áis"
                PersonaGramatical.ELLOS -> "${raiz}an"
            }
            "er", "ir" -> when (persona) {
                PersonaGramatical.YO -> "${raiz}o"
                PersonaGramatical.TU -> "${raiz}es"
                PersonaGramatical.EL -> "${raiz}e"
                PersonaGramatical.NOSOTROS -> if (terminacion == "er") "${raiz}emos" else "${raiz}imos"
                PersonaGramatical.VOSOTROS -> if (terminacion == "er") "${raiz}éis" else "${raiz}ís"
                PersonaGramatical.ELLOS -> "${raiz}en"
            }
            else -> v
        }
    }

    private fun esInfinitivo(p: String): Boolean {
        return p.endsWith("ar") || p.endsWith("er") || p.endsWith("ir")
    }

    private fun procesarPredicadoConVerbo(verbo: String, predicado: String, persona: PersonaGramatical): String {
        return when (verbo) {
            "ir" -> aplicarPreposicionLugar(predicado)
            "jugar" -> aplicarPreposicionDeporte(predicado)
            "querer", "necesitar", "poder" -> {
                when {
                    esEmocion(predicado) -> "estar $predicado"
                    esBebida(predicado) -> "tomar $predicado"
                    esComida(predicado) -> "comer $predicado"
                    else -> predicado
                }
            }
            else -> predicado
        }
    }

    private fun detectarIntencion(predicado: String): Pair<String, String> {
        return when {
            esEmocion(predicado) -> "sentir" to predicado
            esNecesidadFisica(predicado) -> "tener" to predicado
            esDeporte(predicado) -> "jugar" to aplicarPreposicionDeporte(predicado)
            esLugar(predicado) -> "ir" to aplicarPreposicionLugar(predicado)
            esBebida(predicado) -> "tomar" to predicado
            esComida(predicado) -> "comer" to predicado
            else -> "querer" to predicado
        }
    }

    private fun aplicarPreposicionLugar(lugar: String): String {
        val l = lugar.trim()
        val femeninos = setOf("casa", "escuela", "oficina", "iglesia", "tienda")
        val masculinos = setOf("parque", "baño", "médico", "hospital", "supermercado", "colegio", "trabajo")
        return when {
            l in femeninos -> "a la $l"
            l in masculinos -> "al $l"
            else -> "a $l"
        }
    }

    private fun aplicarPreposicionDeporte(deporte: String): String {
        val d = deporte.trim()
        val deportesAl = setOf("fútbol", "futbol", "tenis", "baloncesto", "básquet", "vóley", "ajedrez")
        return if (d in deportesAl) "al $d" else "a $d"
    }

    private fun limpiarVerbo(verbo: String): String = verbo.lowercase().trim().removeSuffix("se")

    private fun adaptarAdjetivoAPlural(palabra: String): String {
        val adjetivo = palabra.lowercase().trim()
        return when {
            adjetivo.endsWith("z") -> adjetivo.dropLast(1) + "ces"
            adjetivo.endsWith("ón") -> adjetivo.dropLast(2) + "ones"
            adjetivo.endsWith("a") -> adjetivo.dropLast(1) + "as"
            adjetivo.endsWith("o") -> adjetivo.dropLast(1) + "os"
            adjetivo.matches(Regex(".*[aeiou]$")) -> adjetivo + "s"
            adjetivo == "feliz" -> "felices"  // Caso especial
            else -> adjetivo + "es"
        }
    }

    private fun esEmocion(p: String): Boolean =
        Regex("feliz|triste|enojado|sorprendido|enojada|cansado|cansada|bien|mal|contento|contenta|enfermo|enferma|asustado|asustada|nervioso|nerviosa|tranquilo|tranquila|aburrido|aburrida|satisfecho|preocupado|alegre").containsMatchIn(p)

    private fun esLugar(p: String): Boolean =
        Regex("casa|escuela|colegio|parque|baño|médico|dentista|hospital|supermercado|trabajo|oficina").containsMatchIn(p)

    private fun esDeporte(p: String): Boolean =
        Regex("fútbol|futbol|tenis|baloncesto|básquet|vóley|juego|pelota|natación").containsMatchIn(p)

    private fun esNecesidadFisica(p: String): Boolean =
        Regex("hambre|sed|sueño|frío|calor|dolor").containsMatchIn(p)

    private fun esComida(p: String): Boolean =
        Regex("sopa|manzana|pan|pizza|arroz|fruta|comida|carne|pollo|pescado|ensalada|yogur|galleta|huevo|queso|pastel").containsMatchIn(p)

    private fun esBebida(p: String): Boolean =
        Regex("agua|jugo|leche|refresco|té|cafe|café|batido|soda|bebida|cerveza|vino").containsMatchIn(p)
}