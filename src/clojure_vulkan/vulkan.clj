(ns clojure-vulkan.vulkan
  (:require [clojure-vulkan.debug :as debug]
            [clojure-vulkan.globals :as globals]
            [clojure-vulkan.instance :as instance]
            [clojure-vulkan.logical-device-and-queue :as logical-device-and-queue]
            [clojure-vulkan.physical-device :as physical-device]
            [clojure-vulkan.validation-layers :as validation-layers]))

(defn init []
  (instance/create)
  (debug/setup-debug-messenger)
  (physical-device/pick-physical-device)
  (println "QUEUE FAMILY: " clojure-vulkan.globals/queue-families)
  (logical-device-and-queue/create-logical-device))

(defn terminate []
  (when validation-layers/*enable-validation-layers*
    (debug/destroy-debug-messenger nil))
  (instance/destroy)
  (globals/reset-queue-families))