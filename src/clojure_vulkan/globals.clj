(ns clojure-vulkan.globals
  (:import (clojure_vulkan.Vulkan Buffer Texture)))

(defmacro set-global! [global-var new-value]
  `(alter-var-root (var ~global-var) (constantly ~new-value)))

(def ^:dynamic *config* {})

(def ^Buffer VERTEX-BUFFER nil)
(def ^Buffer INDEX-BUFFER nil)

(def ^Texture TEXTURE (Texture.))