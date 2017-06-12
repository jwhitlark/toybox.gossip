(defproject toybox.clj.gossip "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-beta2"]
                 [prismatic/schema "0.4.2"]
                 [org.flatland/useful "0.11.3"]
                 [com.cognitect/transit-clj "0.8.271"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 ]

  :profiles {:uberjar {:aot :all}
             :dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.10"]
                                  [org.clojure/java.classpath "0.2.2"]
                                  [criterium "0.4.3"]
                                  [org.clojure/tools.trace "0.7.8"]
                                  [org.clojure/test.check "0.8.0-ALPHA"]]}}

  :main toybox.clj.gossip
  )
