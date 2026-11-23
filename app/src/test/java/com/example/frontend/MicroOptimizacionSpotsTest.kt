package com.example.frontend

import com.example.frontend.data.sampleSpots
import com.example.frontend.model.CandidatoMejorParaTi
import com.example.frontend.model.PeerReview
import com.example.frontend.model.SitioMejorParaTi
import com.example.frontend.model.Spot
import com.example.frontend.model.TasteProfile
import com.example.frontend.model.armarSpotsParaUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Micro-optimizacion del Sprint 4: mide cuanta memoria pide y cuanto tarda armar la lista de la UI,
// con la version de antes (copiada aqui tal cual estaba en el ViewModel) y con armarSpotsParaUi.
class MicroOptimizacionSpotsTest {

    private val perfil = TasteProfile()
    private val resena = PeerReview("You", "ME", "Waiting to be posted", 5, "Great", "just now", 0)

    // Catalogo de prueba: los 7 del catalogo empaquetado repetidos hasta `n` restaurantes (ids distintos).
    private fun catalogo(n: Int): List<Spot> = List(n) { i -> sampleSpots[i % sampleSpots.size].let { it.copy(id = "${it.id}-$i") } }

    private fun ranking(catalogo: List<Spot>) = catalogo.mapIndexed { i, s ->
        SitioMejorParaTi(
            CandidatoMejorParaTi(s.id, s.name, s.emoji, s.cuisine, s.rating, s.isBudget, s.isVegetarian, 5, 10, true),
            compatibilidad = (i * 13) % 101, puntajeEspera = 80, puntaje = 70.0, rank = i + 1,
        )
    }

    // ---- La version de antes, igual que el getter que habia en CampusBitesViewModel ----
    private fun armarAntes(
        catalogo: List<Spot>,
        guardados: Set<String>,
        ranking: List<SitioMejorParaTi>,
        perfil: TasteProfile,
        resenasLocales: Map<String, List<PeerReview>>,
    ): List<Spot> {
        val compatibilidades: () -> Map<String, Int> = { ranking.associate { it.candidato.spotId to it.compatibilidad } }
        fun Spot.conResenasLocales(): Spot {
            val propias = resenasLocales[id].orEmpty()
            return if (propias.isEmpty()) this else copy(reviews = propias + reviews, totalReviews = totalReviews + propias.size)
        }
        return catalogo.map { spot ->
            spot.conResenasLocales().copy(
                isSaved = spot.id in guardados,
                affinityPercent = compatibilidades()[spot.id] ?: perfil.compatibilidad(spot),
            )
        }
    }

    @Test
    fun daExactamenteLoMismoQueAntes() {
        val cat = catalogo(30)
        val guardados = cat.filterIndexed { i, _ -> i % 3 == 0 }.map { it.id }.toSet()
        val rank = ranking(cat).take(20)   // algunos sin ranking: usan la compatibilidad del perfil
        val locales = mapOf(cat[4].id to listOf(resena))
        assertEquals(armarAntes(cat, guardados, rank, perfil, locales), armarSpotsParaUi(cat, guardados, rank, perfil, locales))
    }

    @Test
    fun pideMenosMemoriaPorCalculo() {
        // El contador de memoria del hilo es de la JVM (com.sun.management), no del SDK de Android. Se busca
        // por reflexion para que Android Studio no lo marque como error al analizar los tests contra el SDK.
        val bean = Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean").invoke(null)
        val contador = Class.forName("com.sun.management.ThreadMXBean").getMethod("getCurrentThreadAllocatedBytes")
        fun bytesDelHilo(): Long = contador.invoke(bean) as Long
        println("restaurantes | bytes antes | bytes despues | ahorro | us antes | us despues")
        for (n in listOf(7, 30, 100)) {
            val cat = catalogo(n)
            val guardados = cat.take(n / 2).map { it.id }.toSet()
            val rank = ranking(cat)
            val locales = mapOf(cat[0].id to listOf(resena))
            val repeticiones = 2_000

            fun medir(f: () -> List<Spot>): Pair<Long, Long> {
                repeat(500) { f() }   // calentamiento del JIT
                val bytes0 = bytesDelHilo()
                val t0 = System.nanoTime()
                repeat(repeticiones) { f() }
                val ns = System.nanoTime() - t0
                return (bytesDelHilo() - bytes0) / repeticiones to ns / repeticiones
            }

            val (bytesAntes, nsAntes) = medir { armarAntes(cat, guardados, rank, perfil, locales) }
            val (bytesDespues, nsDespues) = medir { armarSpotsParaUi(cat, guardados, rank, perfil, locales) }
            println("%12d | %11d | %13d | %5.1f%% | %8.1f | %10.1f".format(
                n, bytesAntes, bytesDespues, 100.0 * (bytesAntes - bytesDespues) / bytesAntes, nsAntes / 1000.0, nsDespues / 1000.0))
            assertTrue("con $n restaurantes deberia pedir menos memoria", bytesDespues < bytesAntes)
        }
    }
}
