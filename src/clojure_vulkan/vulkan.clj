(ns clojure-vulkan.vulkan
  (:require [clojure-vulkan.debug :as debug]
            [clojure-vulkan.globals :as globals]
            [clojure-vulkan.graphics-pipeline :as graphics-pipeline]
            [clojure-vulkan.image-views :as image-views]
            [clojure-vulkan.instance :as instance]
            [clojure-vulkan.logical-device-and-queue :as logical-device-and-queue]
            [clojure-vulkan.physical-device :as physical-device]
            [clojure-vulkan.render-pass :as render-pass]
            [clojure-vulkan.swap-chain :as swap-chain]
            [clojure-vulkan.validation-layers :as validation-layers]
            [clojure-vulkan.window-surface :as window-surface]))

(defn init []
  (instance/create)
  (debug/setup-debug-messenger)
  (window-surface/create-surface)
  (physical-device/pick-physical-device)
  (logical-device-and-queue/create-logical-device)
  (swap-chain/create-swap-chain)
  (image-views/create-image-views)
  (render-pass/create-render-pass)
  (graphics-pipeline/create-graphics-pipeline))

(defn find-resets []
  (->> (find-ns 'clojure-vulkan.globals)
       ns-publics
       keys
       (filter (fn [v]
                 (clojure.string/starts-with? v "reset-")))
       (map (fn [sym]
              (symbol "globals" (name sym))))))

(defn terminate []
  (graphics-pipeline/destroy-pipeline-layout)
  (render-pass/destroy-render-pass)
  (image-views/destroy-image-views)
  (swap-chain/destroy-swapchain)
  (window-surface/destroy-surface)
  (logical-device-and-queue/destroy-logical-device)
  (when validation-layers/*enable-validation-layers*
    (debug/destroy-debug-messenger nil))
  (instance/destroy)
  (globals/reset-queue-families)
  (globals/reset-swap-chain-support-details))

