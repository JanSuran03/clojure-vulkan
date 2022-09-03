(ns clojure-vulkan.globals
  (:import (clojure_vulkan.Vulkan Buffer Texture)
           (org.lwjgl.vulkan VK13)))

(defmacro set-global! [global-var new-value]
  `(alter-var-root (var ~global-var) (constantly ~new-value)))

(defn- reset-to-vk-null [_] VK13/VK_NULL_HANDLE)

(def ^:dynamic *config* {})

(def ^Buffer VERTEX-BUFFER nil)
(def ^Buffer INDEX-BUFFER nil)

(def DESCRIPTOR-POOL-POINTER VK13/VK_NULL_HANDLE)
(defn reset-descriptor-pool-ptr [] (alter-var-root #'DESCRIPTOR-POOL-POINTER reset-to-vk-null))

(def ^Long DESCRIPTOR-SET-LAYOUT-POINTER VK13/VK_NULL_HANDLE)
(defn reset-descriptor-set-layout-ptr [] (alter-var-root #'DESCRIPTOR-SET-LAYOUT-POINTER reset-to-vk-null))

(def DESCRIPTOR-SET-POINTERS [])

(def ^Texture TEXTURE (Texture.))