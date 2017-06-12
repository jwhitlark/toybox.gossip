(ns toybox.clj.gossip
  (:require [clojure.set :as set]
            [clojure.pprint :as pp :refer [pprint print-table]]
            [toybox.clj.transport :as trp])

  (:gen-class))

#_ (trp/send! (first @nodes) "Meow!" 4004)

(def startup-failure-chance 0.1)
(def send-failure-chance 0.1)
(def beta 2)

(def clock (atom 0))

(defn tick! [] (swap! clock inc))
(defn reset-clock! [] (reset! clock 0))

(defn atom? [x] (instance? clojure.lang.Atom x))

;; member list
(def members (atom #{{:port 4000 :payload nil :tick 0}
                     {:port 4001 :payload nil :tick 0}
                     {:port 4002 :payload nil :tick 0}
                     {:port 4003 :payload nil :tick 0}
                     {:port 4004 :payload nil :tick 0}
                     {:port 4005 :payload nil :tick 0}
                     {:port 4006 :payload nil :tick 0}
                     {:port 4007 :payload nil :tick 0}
                     {:port 4008 :payload nil :tick 0}
                     {:port 4009 :payload nil :tick 0}
                     {:port 4010 :payload nil :tick 0}
                     }))

(defonce nodes (atom []))

(defn members-valid?
  "Just being a set takes care of most of it."
  [members]
  (let [members (if (atom? members) @members members)]
    (and
     ;; no duplicate port numbers
     (= (count members)
        (count (set (map :port members))))
     ;; valid :tick
     (->> members (map :tick) set (every? integer?))
     ;; valid :payload
     (->> members (map :payload) set (every? #(or (nil? %) (string? %)))))))

(set-validator! members members-valid?)

(defn sample-members
  "randomly get x members, but not self"
  [own-port]
  (let [others (set/select #(not= (:port %) own-port) @members)]
    (take beta (shuffle others))))

(defn assoc-payload [v m] (assoc m :payload v))
(defn update-tick [f m] (update m :tick f))

(defn update-member [members port update-fn]
  (let [me (first (set/select #(= port (:port %)) members))
        less-me (disj members me)]
    (conj less-me (update-fn me))))

;; TODO: push, pull

(def startup-fail? #(< (rand) startup-failure-chance))
(def send-fail? #(< (rand) send-failure-chance))

;; to be called in the per node watch
(defn on-tick [port sock my-tick my-payload]
  ;; if payload, try to send it
  (if @my-payload
    (let [targets (sample-members port)]
      (doseq [target targets]
        ;;(println (format "Node %d is trying to send %s to node %d" port @my-payload (:port target)))
        (trp/send! sock @my-payload (:port target))
        )))
  ;; try to receive a msg
  (try
    (let [msg-in (trp/receive! sock 30)]
      (when msg-in
        ;;(println (format "Node %d received msg %s on tick %d" port msg-in @my-tick))
        (when-not @my-payload
          (reset! my-payload msg-in)
          (swap! members update-member port (partial assoc-payload msg-in)))))
    (catch Exception ex
      (println ex)))
  (swap! members update-member port (partial update-tick inc))
  (swap! my-tick inc))

(defn make-node [port]
  ;; simulate creation failure here
  (println "Creating node " port)
  (let [my-socket (trp/make-socket port)
        my-tick (atom 0)
        my-payload (atom nil)]

    (add-watch clock (keyword (str "tick-" port))
                (fn [ky ref old new]
                  (future (on-tick port my-socket my-tick my-payload))
                  ))
    my-socket
    )
)
;; start a thread which
;; knows it's host
;; decides if it fails
;; sets it's own tick to 0?
;; adds a watch, that on each tick,
;;   check if it has a payload
;;   selects a random set of other hosts, (not self)
;;   sends it's payload to each host
;;   updates it's payload and tick.
;; starts listening on it's port, UDP
;; when it receives something, updates it's payload and
;; logs that it got a request

(defn start-all-nodes []
  (reset! nodes (map #(make-node (:port %)) @members)))

(defn stop-all-nodes []
  (doseq [node @nodes]
    (.close node))
  (reset! nodes []))


(defn print-status []
  (println (format "Clock at %d" @clock))
  (print-table (sort-by :port @members)))

(add-watch clock :change
            (fn [ky ref old new]
              (cond
                (= new 0) (println "Clock reset")
                :else (println (format "Clock at %d" @clock)))))

(defn -main [& args]
  (println "Started")

  (println "Finished"))
