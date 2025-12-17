package com.example.livesplitlike.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livesplitlike.data.local.db.GroupDao
import com.example.livesplitlike.data.local.db.RunDao
import com.example.livesplitlike.data.local.db.RunTimeDao
import com.example.livesplitlike.data.local.db.SplitTemplateDao
import com.example.livesplitlike.data.local.model.GroupEntity
import com.example.livesplitlike.data.local.model.RunEntity
import com.example.livesplitlike.data.local.model.RunTimeEntity
import com.example.livesplitlike.data.local.model.SplitTemplateEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.livesplitlike.data.ButtonMappingStore
import com.example.livesplitlike.data.keyCodeToName
import com.example.livesplitlike.di.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val groupDao: GroupDao,
    private val splitTemplateDao: SplitTemplateDao,
    private val runDao: RunDao,
    private val runTimeDao: RunTimeDao,
    @ApplicationContext private val appContext: Context // <- agregar si quieres acceso a DataStore aquí
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Estado expuesto a la UI
    private val _userEmail = MutableStateFlow<String?>(auth.currentUser?.email)
    val userEmail: StateFlow<String?> = _userEmail

    private val _photoUrl = MutableStateFlow<String?>(auth.currentUser?.photoUrl?.toString())
    val photoUrl: StateFlow<String?> = _photoUrl

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    private val _isAssigning = MutableStateFlow(false)
    val isAssigningFlow: StateFlow<Boolean> = _isAssigning.asStateFlow()

    fun isAssigning(): Boolean = _isAssigning.value

    fun startAssigning() {
        _isAssigning.value = true
        Log.d("SettingsGm", "startAssigning() called in ViewModel - isAssigning=${_isAssigning.value}")
    }



    // exposicion del mapping actual (null si no hay mapping)
    private val _mappedKeyFlow = MutableStateFlow<Int?>(null)
    val mappedKeyFlow: StateFlow<Int?> = _mappedKeyFlow.asStateFlow()

    fun onAssigningFinished(keyCode: Int) {
        _isAssigning.value = false
        _mappedKeyFlow.value = keyCode
        _message.value = "Botón asignado: ${keyCodeToName(keyCode)}"
        Log.d("SettingsGm", "onAssigningFinished keyCode=$keyCode mappedName=${keyCodeToName(keyCode)}")
        // opcional: persistir desde aquí si no lo hizo MainActivity
    }


    // Si quieres que al iniciar el ViewModel se cargue la asignación persistida, usa este init:
    init {
        // Solo si inyectaste appContext en el constructor.
        viewModelScope.launch {
            try {
                val mapped = ButtonMappingStore.getMapping(appContext) // suspend
                _mappedKeyFlow.value = mapped
            } catch (e: Exception) {
                // opcional: log o manejo de error
            }
        }
    }

    init {
        // Mantener sincronizado el estado cuando cambia la auth
        auth.addAuthStateListener { a ->
            _userEmail.value = a.currentUser?.email
            _photoUrl.value = a.currentUser?.photoUrl?.toString()
        }
    }

    // --- Mantengo los métodos email/password por compatibilidad (la UI nueva los oculta) ---
    fun signIn(email: String, password: String) {
        _isLoading.value = true
        _message.value = null
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _userEmail.value = auth.currentUser?.email
                    _photoUrl.value = auth.currentUser?.photoUrl?.toString()
                    _message.value = "Sesión iniciada"
                } else {
                    _message.value = task.exception?.localizedMessage ?: "Error al iniciar sesión"
                }
            }
    }

    fun signUp(email: String, password: String) {
        _isLoading.value = true
        _message.value = null
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _userEmail.value = auth.currentUser?.email
                    _photoUrl.value = auth.currentUser?.photoUrl?.toString()
                    _message.value = "Cuenta creada y vinculada"
                } else {
                    _message.value = task.exception?.localizedMessage ?: "Error al crear cuenta"
                }
            }
    }

    /**
     * Sign in with Google ID token (called from UI after GoogleSignIn returns idToken).
     * Updates userEmail and photoUrl stateflows on success.
     */
    fun signInWithGoogleIdToken(idToken: String?, onDone: ((Boolean, String?) -> Unit)? = null) {
        if (idToken.isNullOrBlank()) {
            onDone?.invoke(false, "ID token nulo")
            return
        }

        _isLoading.value = true
        _message.value = null

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _userEmail.value = auth.currentUser?.email
                    _photoUrl.value = auth.currentUser?.photoUrl?.toString()
                    _message.value = "Sesión iniciada con Google"
                    onDone?.invoke(true, null)
                } else {
                    _userEmail.value = null
                    _photoUrl.value = null
                    _message.value = task.exception?.localizedMessage ?: "Error al iniciar con Google"
                    onDone?.invoke(false, _message.value)
                }
            }
    }

    /**
     * Sign out from Firebase. UI should also call googleSignInClient.signOut() to fully sign out Google account
     * from the GoogleSignInClient (this class cannot access GoogleSignInClient because it needs Context).
     */
    fun signOut() {
        auth.signOut()
        _userEmail.value = null
        _photoUrl.value = null
        _message.value = "Sesión cerrada"
    }

    // --- Upload local DB to Firestore (overwrite) ---
    // Requiere: import kotlinx.coroutines.tasks.await
//           import kotlinx.coroutines.withContext
    fun uploadAllData() {
        val user = auth.currentUser ?: run {
            _message.value = "No hay usuario vinculado"
            return
        }
        _isLoading.value = true
        _message.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1) Leer base local (métodos sync que ya tienes)
                val groups = groupDao.getAllSync()
                val templates = splitTemplateDao.getAllSync()
                val runs = runDao.getAllSync()
                val runtimes = runTimeDao.getAllSync()

                Log.d("SettingsVM", "Iniciando upload: groups=${groups.size}, templates=${templates.size}, runs=${runs.size}, runtimes=${runtimes.size}")

                // 2) Construir payload (idéntico a tu lógica original)
                val payload = hashMapOf<String, Any>(
                    "groups" to groups.map { g -> mapOf("id" to g.id, "name" to g.name) },
                    "templates" to templates.map { t ->
                        mapOf(
                            "id" to t.id,
                            "groupId" to t.groupId,
                            "index" to t.indexInGroup,
                            "name" to t.name
                        )
                    },
                    "runs" to runs.map { r ->
                        mapOf(
                            "id" to r.id,
                            "groupId" to r.groupId,
                            "createdAt" to r.createdAtMillis
                        )
                    },
                    "runtimes" to runtimes.map { rt ->
                        mapOf(
                            "runId" to rt.runId,
                            "groupId" to rt.groupId,
                            "splitIndex" to rt.splitIndex,
                            "time" to rt.timeFromStartMillis,
                            "recordedAt" to rt.recordedAtMillis
                        )
                    }
                )

                Log.d("SettingsVM", "Datos seteados en payload")
                // 3) Escritura en Firestore (esperamos con await)
                val docRef = firestore.collection("users")
                    .document(user.uid)
                    .collection("backups")
                    .document("latest")

                Log.d("SettingsVM", "Datos enviados a Firestore, esperando con await")

                // await() lanzará excepción si falla, o completará si tiene éxito
                docRef.set(payload).await()
                Log.d("SettingsVM", "Upload completado OK")

                // 4) Actualizar estado en el hilo Main para que Compose lo reciba inmediatamente
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _message.value = "Datos subidos correctamente"
                }
            } catch (e: Exception) {
                // Si algo falla, informamos y apagamos el loader en Main
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _message.value = e.localizedMessage ?: "Error al subir datos"
                }
                Log.e("SettingsVM", "Error al subir datos", e)
            }
        }
    }

    // --- Download cloud data and overwrite local DB ---
    fun downloadAllData() {
        val user = auth.currentUser ?: run {
            _message.value = "No hay usuario vinculado"
            return
        }
        _isLoading.value = true
        _message.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val docRef = firestore.collection("users")
                    .document(user.uid)
                    .collection("backups")
                    .document("latest")

                val snapshot = docRef.get().await()
                if (!snapshot.exists()) {
                    _isLoading.value = false
                    _message.value = "No hay respaldo en la nube"
                    return@launch
                }

                val data = snapshot.data ?: emptyMap<String, Any>()

                // 1) delete existing local data
                runTimeDao.deleteAll()
                runDao.deleteAll()
                splitTemplateDao.deleteAll()
                groupDao.deleteAll()

                // 2) insert groups
                val groupsList = (data["groups"] as? List<Map<String, Any>>)?.map { m ->
                    GroupEntity(
                        id = (m["id"] as? Number)?.toLong() ?: 0L,
                        name = m["name"] as? String ?: ""
                    )
                } ?: emptyList()
                groupsList.forEach { groupDao.insertGroup(it) }

                // 3) insert templates
                val templatesList = (data["templates"] as? List<Map<String, Any>>)?.map { m ->
                    SplitTemplateEntity(
                        id = (m["id"] as? Number)?.toLong() ?: 0L,
                        groupId = (m["groupId"] as? Number)?.toLong() ?: 0L,
                        indexInGroup = (m["index"] as? Number)?.toInt() ?: 0,
                        name = m["name"] as? String ?: ""
                    )
                } ?: emptyList()
                templatesList.forEach { splitTemplateDao.insertTemplate(it) }

                // 4) insert runs
                val runsList = (data["runs"] as? List<Map<String, Any>>)?.map { m ->
                    RunEntity(
                        id = (m["id"] as? Number)?.toLong() ?: 0L,
                        groupId = (m["groupId"] as? Number)?.toLong() ?: 0L,
                        createdAtMillis = (m["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                runsList.forEach { runDao.insertRun(it) }

                // 5) insert runtimes
                val runtimesList = (data["runtimes"] as? List<Map<String, Any>>)?.map { m ->
                    RunTimeEntity(
                        id = 0L,
                        runId = (m["runId"] as? Number)?.toLong() ?: 0L,
                        groupId = (m["groupId"] as? Number)?.toLong() ?: 0L,
                        splitIndex = (m["splitIndex"] as? Number)?.toInt() ?: 0,
                        timeFromStartMillis = (m["time"] as? Number)?.toLong() ?: 0L,
                        recordedAtMillis = (m["recordedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                runtimesList.forEach { runTimeDao.insertRunTime(it) }

                _isLoading.value = false
                _message.value = "Datos descargados y aplicados localmente"
            } catch (e: Exception) {
                _isLoading.value = false
                _message.value = e.localizedMessage ?: "Error al descargar datos"
            }
        }
    }

    private val settingsStore = SettingsDataStore(appContext)

    // Exponer un StateFlow para el tema (útil para UI)
    val isDarkThemeFlow: StateFlow<Boolean> = settingsStore.isDarkThemeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // Toggle desde ViewModel
    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setDarkTheme(enabled)
        }
    }

}
