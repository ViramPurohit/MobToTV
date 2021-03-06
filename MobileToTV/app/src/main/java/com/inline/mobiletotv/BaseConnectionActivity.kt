package com.inline.mobiletotv

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import java.util.*


abstract class BaseConnectionActivity : AppCompatActivity() {
    private  var opponentEndpointId: String? = null


    /** Our handler to Nearby Connections.  */
    private var mConnectionsClient: ConnectionsClient? = null

    /** The devices we've discovered near us.  */
    private val mDiscoveredEndpoints: MutableMap<String, Endpoint> =
        HashMap()

    /**
     * The devices we have pending connections to. They will stay pending until we call [ ][.acceptConnection] or [.rejectConnection].
     */
    private val mPendingConnections: MutableMap<String, Endpoint> =
        HashMap()

    /**
     * The devices we are currently connected to. For advertisers, this may be large. For discoverers,
     * there will only be one entry in this map.
     */
    private val mEstablishedConnections: MutableMap<String, Endpoint?> =
        HashMap()

    /** Returns `true` if we're currently attempting to connect to another device.  */
    /**
     * True if we are asking a discovered device to connect to us. While we ask, we cannot ask another
     * device.
     */
    protected var isConnecting = false
        private set

    /** Returns `true` if currently discovering.  */
    /** True if we are discovering.  */
     var isDiscovering = false
        private set

    /** Returns `true` if currently advertising.  */
    /** True if we are advertising.  */
    protected var isAdvertising = false
        private set

    /** Callbacks for connections to other devices.  */
    private val mConnectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(
                endpointId: String,
                connectionInfo: ConnectionInfo
            ) {
                logD(
                    java.lang.String.format(
                        "onConnectionInitiated(endpointId=%s, endpointName=%s)",
                        endpointId, connectionInfo.getEndpointName()
                    )
                )
                val endpoint =
                    Endpoint(endpointId, connectionInfo.getEndpointName())
                mPendingConnections[endpointId] = endpoint
                this@BaseConnectionActivity.onConnectionInitiated(endpoint, connectionInfo)
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {
                logD(
                    java.lang.String.format(
                        "onConnectionResponse(endpointId=%s, result=%s)",
                        endpointId,
                        result
                    )
                )

                // We're no longer connecting
                isConnecting = false
                if (!result.getStatus().isSuccess()) {
                    logW(
                        String.format(
                            "Connection failed. Received status %s.",
                            toString(result.getStatus())
                        )
                    )

                    onConnectionFailed(mPendingConnections.remove(endpointId))
                    return
                }
                if (result.getStatus().isSuccess()) {
                    opponentEndpointId = endpointId
                }
                connectedToEndpoint(mPendingConnections.remove(endpointId))
            }

            override fun onDisconnected(endpointId: String) {
                if (!mEstablishedConnections.containsKey(endpointId)) {
                    logW("Unexpected disconnection from endpoint $endpointId")
                    return
                }
                disconnectedFromEndpoint(mEstablishedConnections[endpointId])
            }
        }

    /** Callbacks for payloads (bytes of data) sent from another device to us.  */
    private val mPayloadCallback: PayloadCallback = object : PayloadCallback() {


        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            logD(
                java.lang.String.format(
                    "onPayloadReceived(endpointId=%s, payload=%s)",
                    endpointId,
                    payload
                )
            )
            onReceive(mEstablishedConnections[endpointId], payload)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            logD(
                java.lang.String.format(
                    "onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update
                )
            )
        }
    }

    /** Called when our Activity is first created.  */
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mConnectionsClient = Nearby.getConnectionsClient(this)
    }

    /** Called when our Activity has been made visible to the user.  */
    override fun onStart() {
        super.onStart()
        if (!hasPermissions(this, *requiredPermissions)) {
            if (!hasPermissions(this, *requiredPermissions)) {
                if (Build.VERSION.SDK_INT < 23) {
                    ActivityCompat.requestPermissions(
                        this,
                        requiredPermissions,
                        REQUEST_CODE_REQUIRED_PERMISSIONS
                    )
                } else {
                    requestPermissions(
                        requiredPermissions,
                        REQUEST_CODE_REQUIRED_PERMISSIONS
                    )
                }
            }
        }
    }

    @CallSuper
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            for (grantResult in grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG)
                        .show()
                    finish()
                    return
                }
            }
            recreate()
        }
        super.onRequestPermissionsResult(requestCode, permissions!!, grantResults)
    }

    /**
     * Sets the device to advertising mode. It will broadcast to other devices in discovery mode.
     * Either [.onAdvertisingStarted] or [.onAdvertisingFailed] will be called once
     * we've found out if we successfully entered this mode.
     */
    protected fun startAdvertising() {
        isAdvertising = true
        val localEndpointName = name
        val advertisingOptions: AdvertisingOptions.Builder = AdvertisingOptions.Builder()
        advertisingOptions.setStrategy(strategy)
        mConnectionsClient
            ?.startAdvertising(
                localEndpointName,
                serviceId,
                mConnectionLifecycleCallback,
                advertisingOptions.build()
            )
            ?.addOnSuccessListener(
                object : OnSuccessListener<Void?> {
                    override fun onSuccess(unusedResult: Void?) {
                        logV("Now advertising endpoint $localEndpointName")
                        onAdvertisingStarted()
                    }
                })
            ?.addOnFailureListener(
                object : OnFailureListener {
                    override fun onFailure(@NonNull e: Exception) {
                        isAdvertising = false
                        logW("startAdvertising() failed.", e)
                        onAdvertisingFailed()
                    }
                })
    }

    /** Stops advertising.  */
    protected fun stopAdvertising() {
        isAdvertising = false
        mConnectionsClient?.stopAdvertising()
    }

    /** Called when advertising successfully starts. Override this method to act on the event.  */
    protected fun onAdvertisingStarted() {}

    /** Called when advertising fails to start. Override this method to act on the event.  */
    protected fun onAdvertisingFailed() {}

    /**
     * Called when a pending connection with a remote endpoint is created. Use [ConnectionInfo]
     * for metadata about the connection (like incoming vs outgoing, or the authentication token). If
     * we want to continue with the connection, call [.acceptConnection]. Otherwise,
     * call [.rejectConnection].
     */
    protected open fun onConnectionInitiated(
        endpoint: Endpoint?,
        connectionInfo: ConnectionInfo?
    ) {
    }

    /** Accepts a connection request.  */
    protected fun acceptConnection(endpoint: Endpoint) {
        mConnectionsClient
            ?.acceptConnection(endpoint.id, mPayloadCallback)
            ?.addOnFailureListener(
                object : OnFailureListener {
                    override fun onFailure(@NonNull e: Exception) {
                        logW("acceptConnection() failed.", e)
                    }
                })
    }

    /** Rejects a connection request.  */
    protected fun rejectConnection(endpoint: Endpoint) {
        mConnectionsClient
            ?.rejectConnection(endpoint.id)
            ?.addOnFailureListener(
                object : OnFailureListener {
                    override fun onFailure(@NonNull e: Exception) {
                        logW("rejectConnection() failed.", e)
                    }
                })
    }

    /**
     * Sets the device to discovery mode. It will now listen for devices in advertising mode. Either
     * [.onDiscoveryStarted] or [.onDiscoveryFailed] will be called once we've found
     * out if we successfully entered this mode.
     */
    protected fun startDiscovering() {
        isDiscovering = true
        mDiscoveredEndpoints.clear()
        val discoveryOptions: DiscoveryOptions.Builder = DiscoveryOptions.Builder()
        discoveryOptions.setStrategy(strategy)
        mConnectionsClient
            ?.startDiscovery(
                serviceId,
                object : EndpointDiscoveryCallback() {
                    override fun onEndpointFound(
                        endpointId: String,
                        info: DiscoveredEndpointInfo
                    ) {
                        logD(
                            java.lang.String.format(
                                "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                endpointId, info.getServiceId(), info.getEndpointName()
                            )
                        )
                        if (serviceId == info.getServiceId()) {
                            val endpoint =
                                Endpoint(endpointId, info.getEndpointName())
                            mDiscoveredEndpoints[endpointId] = endpoint
                            onEndpointDiscovered(endpoint)
                        }
                    }

                    override fun onEndpointLost(endpointId: String) {
                        logD(String.format("onEndpointLost(endpointId=%s)", endpointId))
                    }
                },
                discoveryOptions.build()
            )
            ?.addOnSuccessListener(
                object : OnSuccessListener<Void?> {
                    override fun onSuccess(unusedResult: Void?) {
                        onDiscoveryStarted()
                    }
                })
            ?.addOnFailureListener(
                object : OnFailureListener {
                    override fun onFailure(@NonNull e: Exception) {
                        isDiscovering = false
                        logW("startDiscovering() failed.", e)
                        onDiscoveryFailed()
                    }
                })
    }

    /** Stops discovery.  */
    protected fun stopDiscovering() {
        isDiscovering = false
        mConnectionsClient?.stopDiscovery()
    }

    /** Called when discovery successfully starts. Override this method to act on the event.  */
    protected fun onDiscoveryStarted() {}

    /** Called when discovery fails to start. Override this method to act on the event.  */
    protected fun onDiscoveryFailed() {}

    /**
     * Called when a remote endpoint is discovered. To connect to the device, call [ ][.connectToEndpoint].
     */
    protected open fun onEndpointDiscovered(endpoint: Endpoint?) {}

    /** Disconnects from the given endpoint.  */
    protected fun disconnect(endpoint: Endpoint) {
        mConnectionsClient?.disconnectFromEndpoint(endpoint.id)
        mEstablishedConnections.remove(endpoint.id)
    }

    /** Disconnects from all currently connected endpoints.  */
    protected fun disconnectFromAllEndpoints() {
        for (endpoint in mEstablishedConnections.values) {
            mConnectionsClient?.disconnectFromEndpoint(endpoint!!.id)
        }
        mEstablishedConnections.clear()
    }

    /** Resets and clears all state in Nearby Connections.  */
    protected fun stopAllEndpoints() {
        mConnectionsClient?.stopAllEndpoints()
        isAdvertising = false
        isDiscovering = false
        isConnecting = false
        mDiscoveredEndpoints.clear()
        mPendingConnections.clear()
        mEstablishedConnections.clear()
    }

    /**
     * Sends a connection request to the endpoint. Either [.onConnectionInitiated] or [.onConnectionFailed] will be called once we've found out
     * if we successfully reached the device.
     */
    protected fun connectToEndpoint(endpoint: Endpoint) {
        logV("Sending a connection request to endpoint $endpoint")
        // Mark ourselves as connecting so we don't connect multiple times
        isConnecting = true

        // Ask to connect
        mConnectionsClient
            ?.requestConnection(name, endpoint.id, mConnectionLifecycleCallback)
            ?.addOnFailureListener(
                object : OnFailureListener {
                    override fun onFailure(@NonNull e: Exception) {
                        logW("requestConnection() failed.", e)
                        isConnecting = false
                        onConnectionFailed(endpoint)
                    }
                })
    }

    private fun connectedToEndpoint(endpoint: Endpoint?) {
        logD(String.format("connectedToEndpoint(endpoint=%s)", endpoint))
        mEstablishedConnections[endpoint!!.id] = endpoint
        onEndpointConnected(endpoint)
    }

    private fun disconnectedFromEndpoint(endpoint: Endpoint?) {
        logD(String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint))
        mEstablishedConnections.remove(endpoint!!.id)
        onEndpointDisconnected(endpoint)
    }

    /**
     * Called when a connection with this endpoint has failed. Override this method to act on the
     * event.
     */
    protected open fun onConnectionFailed(endpoint: Endpoint?) {}

    /** Called when someone has connected to us. Override this method to act on the event.  */
    protected open fun onEndpointConnected(endpoint: Endpoint?) {}

    /** Called when someone has disconnected. Override this method to act on the event.  */
    protected open fun onEndpointDisconnected(endpoint: Endpoint?) {}

    /** Returns a list of currently connected endpoints.  */
    protected val discoveredEndpoints: Set<Endpoint>
        protected get() = HashSet(mDiscoveredEndpoints.values)

    /** Returns a list of currently connected endpoints.  */
    protected val connectedEndpoints: Set<Endpoint?>
        protected get() = HashSet(mEstablishedConnections.values)

    /**
     * Sends a [Payload] to all currently connected endpoints.
     *
     * @param payload The data you want to send.
     */
    protected fun send(payload: Payload) {
        opponentEndpointId?.let { send(payload, it) }
    }

    private fun send(
        payload: Payload,
        endpoint: String
    ) {
        mConnectionsClient
            ?.sendPayload(endpoint, payload)
            ?.addOnFailureListener(
                object : OnFailureListener {
                    override fun onFailure(@NonNull e: Exception) {
                        logW("sendPayload() failed.", e)
                    }
                }
            )
            ?.addOnSuccessListener {
                logW("sendPayload() Success.")
            }
    }

    /**
     * Someone connected to us has sent us data. Override this method to act on the event.
     *
     * @param endpoint The sender.
     * @param payload The data.
     */
    protected open fun onReceive(
        endpoint: Endpoint?,
        payload: Payload?
    ) {

    }

    /**
     * An optional hook to pool any permissions the app needs with the permissions ConnectionsActivity
     * will request.
     *
     * @return All permissions required for the app to properly function.
     */
    protected open fun getRequiredPermissions(): Array<String> {
        return requiredPermissions
    }

    /** Returns the client's name. Visible to others when connecting.  */
    protected abstract val name: String

    /**
     * Returns the service id. This represents the action this connection is for. When discovering,
     * we'll verify that the advertiser has the same service id before we consider connecting to them.
     */
    protected abstract val serviceId: String

    /**
     * Returns the strategy we use to connect to other devices. Only devices using the same strategy
     * and service id will appear when discovering. Stragies determine how many incoming and outgoing
     * connections are possible at the same time, as well as how much bandwidth is available for use.
     */
    protected abstract val strategy: Strategy?

    @CallSuper
    protected open fun logV(msg: String?) {
        Log.v(Constants.TAG, msg!!)
    }

    @CallSuper
    protected open fun logD(msg: String?) {
        Log.d(Constants.TAG, msg!!)
    }

    @CallSuper
    protected open fun logW(msg: String?) {
        Log.w(Constants.TAG, msg!!)
    }

    @CallSuper
    protected open fun logW(msg: String?, e: Throwable?) {
        Log.w(Constants.TAG, msg, e)
    }

    @CallSuper
    protected open fun logE(msg: String?, e: Throwable?) {
        Log.e(Constants.TAG, msg, e)
    }

    /** Represents a device we can talk to.  */
    protected class Endpoint(
        @field:NonNull @get:NonNull
        @param:NonNull val id: String,
        @field:NonNull @get:NonNull
        @param:NonNull val name: String
    ) {

        override fun equals(obj: Any?): Boolean {
            if (obj is Endpoint) {
                return id == obj.id
            }
            return false
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }

        override fun toString(): String {
            return String.format("Endpoint{id=%s, name=%s}", id, name)
        }

    }

    companion object {
        /**
         * An optional hook to pool any permissions the app needs with the permissions ConnectionsActivity
         * will request.
         *
         * @return All permissions required for the app to properly function.
         */
        /**
         * These permissions are required before connecting to Nearby Connections. Only [ ][Manifest.permission.ACCESS_COARSE_LOCATION] is considered dangerous, so the others should be
         * granted just by having them in our AndroidManfiest.xml
         */
        protected val requiredPermissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val REQUEST_CODE_REQUIRED_PERMISSIONS = 1

        /**
         * Transforms a [Status] into a English-readable message for logging.
         *
         * @param status The current status
         * @return A readable String. eg. [404]File not found.
         */
        private fun toString(status: Status): String {
            return java.lang.String.format(
                Locale.US,
                "[%d]%s",
                status.getStatusCode(),
                if (status.getStatusMessage() != null) status.getStatusMessage() else ConnectionsStatusCodes.getStatusCodeString(
                    status.getStatusCode()
                )
            )
        }


    }

    /**
     * Returns `true` if the app was granted all the permissions. Otherwise, returns `false`.
     */
    @CallSuper
    open fun hasPermissions(
        context: Context?,
        vararg permissions: String?
    ): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context!!, permission!!)
                !== PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }
}