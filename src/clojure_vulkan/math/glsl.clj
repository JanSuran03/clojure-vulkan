(ns clojure-vulkan.math.glsl
  (:refer-clojure :exclude [deftype float format])
  (:import (org.lwjgl.vulkan VK13)))

(defmacro deftype [type-name fields glsl-type-interface & methods]
  `(clojure.core/deftype ~type-name ~fields
     ~glsl-type-interface
     ~@(map (fn [[method-name argvec return]]
              (list method-name argvec
                    (if (list? return)
                      (apply * (map eval (rest return)))
                      (eval return)))) methods)))

(defprotocol GLSLType
  (components [this])
  (component-sizeof [this])
  (sizeof [this])
  (format [this]))

(deftype GLSLFloat []
  GLSLType
  (components [this] 1)
  (component-sizeof [this] Float/BYTES)
  (sizeof [this] (* 1 Float/BYTES))
  (format [this] VK13/VK_FORMAT_R32_SFLOAT))

(deftype GLSLVec2f []
  GLSLType
  (components [this] 2)
  (component-sizeof [this] Float/BYTES)
  (sizeof [this] (* 2 Float/BYTES))
  (format [this] VK13/VK_FORMAT_R32G32_SFLOAT))

(deftype GLSLVec3f []
  GLSLType
  (components [this] 3)
  (component-sizeof [this] Float/BYTES)
  (sizeof [this] (* 3 Float/BYTES))
  (format [this] VK13/VK_FORMAT_R32G32B32_SFLOAT))

(deftype GLSLVec4f []
  GLSLType
  (components [this] 4)
  (component-sizeof [this] Float/BYTES)
  (sizeof [this] (* 4 Float/BYTES))
  (format [this] VK13/VK_FORMAT_R32G32B32A32_SFLOAT))

(def float (GLSLFloat.))
(def vec2f (GLSLVec2f.))
(def vec3f (GLSLVec3f.))
(def vec4f (GLSLVec4f.))

(defn kw->type [kw]
  (case kw
    :float float
    :vec2 vec2f
    :vec3 vec3f
    :vec4 vec4f))