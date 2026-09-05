package com.iptv.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Emparelhamento real TV <-> telemóvel.
 * A TV gera um código de 6 dígitos e grava-o (já com as credenciais desta
 * sessão) numa coleção temporária no Firestore. O telemóvel (ainda sem
 * sessão) introduz esse código e lê o documento correspondente, entrando
 * automaticamente na conta, sem necessidade de escrever utilizador/senha.
 *
 * Histórico: isto usava um servidor de emparelhamento próprio
 * (rafaiptv2026.duckdns.org:9443), que deixou de existir (o domínio nem
 * sequer resolve mais). Substituído pelo mesmo projeto Firebase já usado
 * para o sync de favoritos/continuar-a-ver, evitando depender de um
 * servidor próprio que já não existe.
 *
 * Nota de segurança: tal como no sistema antigo, um código de 6 dígitos
 * (1 milhão de combinações) tem sempre um risco pequeno de ser adivinhado
 * por força bruta dentro da janela de tempo em que está ativo (10 minutos).
 * As regras do Firestore (firestore.rules) restringem o acesso à coleção
 * "pairing" a documentos cujo ID tenha exatamente 6 dígitos numéricos, e o
 * código é apagado assim que é usado (torna-se inválido de imediato).
 */
object PairingManager {

    private const val CODE_TTL_MS = 10 * 60 * 1000L // 10 minutos

    data class PairedCredentials(val username: String, val password: String)

    private fun db() = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("pairing")

    private fun randomCode(): String = Random.nextInt(100000, 1000000).toString()

    /**
     * Gera um código novo e grava de imediato as credenciais desta sessão
     * associadas a ele, ficando "pronto a usar" por outro dispositivo.
     * Devolve o código ou null em caso de erro (ex: sem ligação à Internet).
     */
    suspend fun generateSelfCode(username: String, password: String): String? = withContext(Dispatchers.IO) {
        try {
            // Tenta até 5 códigos aleatórios diferentes, para o caso (muito raro)
            // de colisão com um código já ativo de outro emparelhamento.
            repeat(5) {
                val code = randomCode()
                val doc = db().document(code)
                val existing = doc.get().await()
                if (!existing.exists()) {
                    val data = hashMapOf<String, Any>(
                        "username" to username,
                        "password" to password,
                        "createdAt" to System.currentTimeMillis()
                    )
                    doc.set(data).await()
                    return@withContext code
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Consulta uma vez se o código existe e ainda é válido.
     * Devolve Triple(pending, credentials, erroFatal):
     * - pending = true -> continuar a fazer polling (ainda não existe / erro temporário)
     * - credentials != null -> emparelhamento concluído com sucesso (código consumido de imediato)
     * - erroFatal = true -> código inválido/expirado, parar polling
     */
    suspend fun pollOnce(code: String): PollResult = withContext(Dispatchers.IO) {
        try {
            val doc = db().document(code)
            val snapshot = doc.get().await()
            if (!snapshot.exists()) {
                return@withContext PollResult(pending = false, credentials = null, expired = true)
            }
            val createdAt = snapshot.getLong("createdAt") ?: 0L
            if (System.currentTimeMillis() - createdAt > CODE_TTL_MS) {
                doc.delete().await()
                return@withContext PollResult(pending = false, credentials = null, expired = true)
            }
            val username = snapshot.getString("username") ?: ""
            val password = snapshot.getString("password") ?: ""
            // Código de uso único: apaga assim que é lido com sucesso.
            doc.delete().await()
            PollResult(pending = false, credentials = PairedCredentials(username, password), expired = false)
        } catch (e: Exception) {
            PollResult(pending = true, credentials = null, expired = false)
        }
    }

    data class PollResult(val pending: Boolean, val credentials: PairedCredentials?, val expired: Boolean)
}
