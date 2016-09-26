(ns solsort.old-3d-photos.old-3d-photos
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]]
   [reagent.ratom :as ratom :refer  [reaction]])
  (:require
   [cljs.reader]
   [solsort.toolbox.setup]
   [solsort.toolbox.appdb :refer [db db! db-async!]]
   [solsort.toolbox.ui :refer [input select]]
   [solsort.util
    :refer
    [<ajax <seq<! js-seq load-style! put!close!
     parse-json-or-nil log page-ready render dom->clj]]
   [reagent.core :as reagent :refer []]
   [clojure.string :as string :refer [replace split blank?]]
   [cljs.core.async :refer [>! <! chan put! take! timeout close! pipe]]))

(go
  (let [files
        (js->clj
         (.slice
          (.split (<! (<ajax "assets/files.lst"
                             :result :text))
                  "\n")
          0 -1))
        ]
    (db! [:files] files)
    (log files)))

(defn process-image [f img]
  (let [c (js/document.getElementById "canvas")
        ctx (.getContext c "2d")
        _ (.drawImage ctx img 0 0)
        data (aget (.getImageData ctx 0 0 2200 1100) "data")
        px (fn [x y] (aget data (+ (* x 4) (* y 4 2200))))
        fit
        (fn [x y step]
          (let [x0 400
                x1 (+ 1300 x)
                y0 500
                y1 (+ 500 y)]
            (loop [x 0
                   y 0
                   total 0]
              (cond
                (> y 500) total
                (> x 500) (recur 0 (+ y step) total)
                :else (recur (+ x step) y
                             (+ total
                                (js/Math.abs
                                 (-
                                  (px (+ x0 x) (+ y0 y))
                                  (px (+ x1 x) (+ y1 y))
                                  ))))))))
        ]
    
    (let
        [[p0 x0 y0]
         (time (log (first (sort (for [x (range -150 150 20)
                                  y (range -150 150 20)]
                              [(fit x y 10) x y]
                              )))))
         [p1 x1 y1]
         (time (log (first (sort (for [x (range -10 10 1)
                                  y (range -10 10 1)]
                              [(fit (+ x0 x) (+ y0 y) 10) (+ x0 x) (+ y0 y)]
                              )))))
         ]
      #_(doto ctx
        (aset "fillStyle" "black")
        (.fillRect 0 0 2200 1100)
        (aset "fillStyle" "red")
        (.fillRect 400 300 500 500)
        (.fillRect (+ 1300 x1) (+ 300 y1) 500 500))
      (db! [:x] (- 2200 1300 (- x1) ))
      (db! [:y] y1))
    (db! [:show] true)
    )
  )
(defn load-image [f]
  (let [img (js/Image.)]
    (js/console.log (aget img "height"))
    
    (aset img "onload" #(process-image f img))
    (aset img "src" (str "assets/orig/" f "_o.jpg"))
    ))
(defonce _
  (js/setInterval #(db! [:flip] (not (db [:flip]))) 100))
(db [:flip])
(def scale
  (* 0.95 (min
    (/ js/window.innerWidth 600)
    (/ js/window.innerHeight 800)
    ))
  #_(min
   (/ js/window.clientWidth 2200)
   (/ js/window.clientHeight 1100)
   )
  )

(aset js/window "onhashchange" #(load-image (.slice js/location.hash 1)))
(defn canvas []
  [:div {:style
         {:position :fixed
          :width (* scale 600)
          :height (* scale 800)
          :left "50%"
          :margin-left (* -1 scale 300)
          :top "50%"
          :margin-top (* -1 scale 400)
          :z-index 100
          :overflow :hidden
          :border "1px solid black"
          :display (if (db [:show])
                      :inline
                      :none)
          :box-shadow "3px 3px 20px rgba(0,0,0,1)"
          }}
   [:canvas {:id "canvas" :width 2200 :height 1100
             :on-click #(do
                          (db! [:show] false)
                          (js/history.replaceState "" "" js/window.location.pathname))
             :style {:width (* scale 2200)
                     :height (* scale 1100)
                     :position :absolute
                    :background :black
                     :left (+ (* -400 scale) (if (db [:flip]) 0 (* -1 scale (db [:x] 0))))
                     :top (+ (* -100 scale) (if (db [:flip]) 0 (* -1 scale (db [:y] 0))))
                    }}]]
  )
(defn main []
  [:center
   [canvas]
   (into [:div]
         (for [f (db [:files])]
           [:span
            {:on-click (fn []
                         (log f)
                         (aset js/location "hash" f)
                         #_(load-image f))
             :style
             {:display :inline-block
              :width 80 :height 80
              :margin-top -4
              :position :relative
              :overflow :hidden}}
            [:img {:src (str "assets/thumbs/" f "_o.jpg")
                   :style
                   {:position :absolute
                    :top -14 :left -26}
                   }]]
           )
         )
   ]
  )
(render [main])
