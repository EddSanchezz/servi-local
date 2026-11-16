package com.servilocal

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.servilocal.core.di.FirebaseModule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class UserProfilePrivacyEmulatorTest {

    @Test
    fun privateUserDocumentsAreOwnerOnlyAndPublicProfilesContainNoEmail() = runBlocking {
        assertTrue("Enable FIREBASE_EMULATOR_ENABLED for this instrumentation test", BuildConfig.FIREBASE_EMULATOR_ENABLED)

        val auth = FirebaseModule.provideFirebaseAuth()
        val firestore = FirebaseModule.provideFirebaseFirestore()
        auth.signOut()
        val suffix = UUID.randomUUID().toString()
        val password = "ServiLocalTest2026"
        val emailA = "privacy-a-$suffix@servilocal.test"
        val emailB = "privacy-b-$suffix@servilocal.test"
        var uidA: String? = null
        var uidB: String? = null

        try {
            uidA = auth.createUserWithEmailAndPassword(emailA, password).await().user?.uid
                ?: error("Could not create first privacy test user")
            createUserDocuments(firestore, uidA!!, emailA, "Perfil público A")

            auth.signOut()
            uidB = auth.createUserWithEmailAndPassword(emailB, password).await().user?.uid
                ?: error("Could not create second privacy test user")
            createUserDocuments(firestore, uidB!!, emailB, "Perfil público B")

            val privateProfile = firestore.collection(USERS_COLLECTION).document(uidB!!).get().await()
            assertEquals(emailB, privateProfile.getString("email"))
            assertTrue(runCatching {
                firestore.collection(USERS_COLLECTION).document(uidA!!).get().await()
            }.isFailure)
            assertTrue(runCatching { firestore.collection(USERS_COLLECTION).get().await() }.isFailure)

            val publicProfile = firestore.collection(PUBLIC_USERS_COLLECTION).document(uidA!!).get().await()
            assertEquals("Perfil público A", publicProfile.getString("name"))
            assertEquals("ACTIVE", publicProfile.getString("status"))
            assertFalse(publicProfile.contains("email"))
            assertFalse(publicProfile.contains("role"))

            val profileUpdate = firestore.batch()
            profileUpdate.update(firestore.collection(USERS_COLLECTION).document(uidB!!), "name", "Nombre actualizado")
            profileUpdate.set(
                firestore.collection(PUBLIC_USERS_COLLECTION).document(uidB!!),
                mapOf("name" to "Nombre actualizado"),
                com.google.firebase.firestore.SetOptions.merge()
            )
            profileUpdate.commit().await()
            assertEquals(
                "Nombre actualizado",
                firestore.collection(PUBLIC_USERS_COLLECTION).document(uidB!!).get().await().getString("name")
            )

            assertTrue(runCatching {
                firestore.collection(PUBLIC_USERS_COLLECTION).document(uidA!!).update("email", emailB).await()
            }.isFailure)
            assertTrue(runCatching {
                firestore.collection(USERS_COLLECTION).document(uidB!!).update("role", "MODERATOR").await()
            }.isFailure)

            auth.signOut()
            assertTrue(runCatching {
                firestore.collection(PUBLIC_USERS_COLLECTION).document(uidA!!).get().await()
            }.isFailure)
        } finally {
            uidB?.let { id ->
                auth.signOut()
                runCatching { auth.signInWithEmailAndPassword(emailB, password).await() }
                runCatching { firestore.collection(PUBLIC_USERS_COLLECTION).document(id).delete().await() }
                runCatching { firestore.collection(USERS_COLLECTION).document(id).delete().await() }
                runCatching { auth.currentUser?.delete()?.await() }
            }
            uidA?.let { id ->
                auth.signOut()
                runCatching { auth.signInWithEmailAndPassword(emailA, password).await() }
                runCatching { firestore.collection(PUBLIC_USERS_COLLECTION).document(id).delete().await() }
                runCatching { firestore.collection(USERS_COLLECTION).document(id).delete().await() }
                runCatching { auth.currentUser?.delete()?.await() }
            }
            auth.signOut()
        }
    }

    private suspend fun createUserDocuments(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        userId: String,
        email: String,
        name: String
    ) {
        val privateReference = firestore.collection(USERS_COLLECTION).document(userId)
        val publicReference = firestore.collection(PUBLIC_USERS_COLLECTION).document(userId)
        val batch = firestore.batch()
        batch.set(privateReference, mapOf(
            "name" to name,
            "email" to email,
            "role" to "USER",
            "points" to 0,
            "level" to "PRINCIPIANTE",
            "status" to "ACTIVE",
            "createdAtMillis" to System.currentTimeMillis(),
            "photoUrl" to null
        ))
        batch.set(publicReference, mapOf(
            "name" to name,
            "photoUrl" to null,
            "status" to "ACTIVE"
        ))
        batch.commit().await()
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val PUBLIC_USERS_COLLECTION = "users_public"
    }
}
