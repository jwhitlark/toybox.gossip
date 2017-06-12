(ns toybox.clj.transport
  (:require [clojure.pprint :as pp :refer [pprint print-table]]
            [cognitect.transit :as transit])
  (:import [java.net DatagramSocket DatagramPacket InetSocketAddress SocketTimeoutException]
           [java.io ByteArrayInputStream ByteArrayOutputStream]))

#_ (receive-loop socket println)
#_ (send socket "hello, world!" "localhost" 8888)

(def make-socket (fn [port] (DatagramSocket. port)))
(def close-socket (fn [socket] (.close socket)))

(defn- transit-write [struct]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer struct)
    ;; (println (.toString out))
    (.toByteArray out)))

(defn- transit-read [packet]
  (let [in (ByteArrayInputStream. (.getData packet))
        reader (transit/reader in :json)]
    (transit/read reader)))

(defn send!
  "Send a transit based message over a DatagramSocket to the specified
  host and port. If the struct is over 4096 bytes long, an exception
  will be thrown."
  ([^DatagramSocket socket struct port]
   (send! socket struct "localhost" port))
  ([^DatagramSocket socket struct host port]
   (let [payload (transit-write struct)
         length (alength payload)]
     (if (> length 4096)
       (throw (Exception. "Payload too big.")))
     (let [address (InetSocketAddress. host port)
           packet (DatagramPacket. payload length address)]
       (.send socket packet)))))

(defn receive!
  "Block until a UDP message is received on the given DatagramSocket, and
  return the payload message as a string."
  [^DatagramSocket socket timeout]
  (let [buffer (byte-array 4096)
        packet (DatagramPacket. buffer 4096)]
    (.setSoTimeout socket timeout)
    (try
      (.receive socket packet)
      (transit-read packet)
      (catch SocketTimeoutException ex
        ;; (println (format "Socket on %d timed out after %d ms." (.getLocalPort socket) timeout))
        nil))))

(defn receive-loop
  "Given a function and DatagramSocket, will (in another thread) wait
  for the socket to receive a message, and whenever it does, will call
  the provided function on the incoming message."
  [socket f]
  (future (while true (f (receive! socket)))))
