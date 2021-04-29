(ns motform.strange.material.styles
  (:require [cljfx.css :as css]))

(def ^:private colors
  {:background "hsb(0, 0%, 100%)"
   :scroll-bar "hsb(0, 0%, 95%)"
   :sidebar    "hsb(0, 0%, 80%)"
   :frame      "hsb(0, 0%, 20%)"

   :inactive   "hsb(0, 0%, 50%)"
   :meta       "hsb(0, 0%, 30%)"
   :foreground "hsb(0, 0%, 0%)"

   :border     "hsb(0, 0%, 80%)"
   :selection  "hsb(0, 0%, 98%)"
   :transparent "hsba(0, 0%, 0%, 0.0)"})

(def styles
  (css/register
   ::style
   (let [padding 10]
     {::padding padding

      "@font-face" {:src "url(\"resources/fonts/MDSystemTest-Regular.otf\")"}

      ".root" {:-fx-background-color (colors :background)
               :-fx-text-fill        "red"
               :-fx-font-size        20}

      ".scroll-bar:vertical"   {:-fx-pref-width  10}
      ".scroll-bar:horizontal" {:-fx-pref-height 10}

      ".track" {:-fx-background-color (colors :sidebar)}
      ".thumb" {:-fx-background-color (colors :scroll-bar) :-fx-background-radius 0}

      ".increment-button" {:-fx-background-color (colors :sidebar) :-fx-padding 0 :-fx-border-color (colors :sidebar) }
      ".decrement-button" {:-fx-background-color (colors :sidebar) :-fx-padding -50 :-fx-border-color (colors :sidebar)}
      ".decrement-arrow"  {:-fx-padding 0 :-fx-border-width 0 :-fx-border-color (colors :sidebar) :-fx-shape " "}
      ".increment-arrow"  {:-fx-padding 0 :-fx-border-width 0 :-fx-border-color (colors :sidebar) :-fx-shape " "}
      ".scroll-arrow"     {:-fx-padding 0}

      ".text-field" {:-fx-background-color (colors :background)
                     :-fx-background-radius 0}

      ".kernel"
      {"-command-line" {:-fx-padding    [0 0 0 0]
                        :-fx-background (colors :sidebar)}

       "-sidebar" {:-fx-font-size    12
                   :-fx-border-color (colors :sidebar)
                   :-fx-border-width [1 0 0 0]

                   "-container" {"-label"
                                 {:-fx-padding          [5 5 5 10]
                                  :-fx-font-size        12
                                  :-fx-background-color (colors :sidebar)}}

                   "-list" {:-fx-background-color (colors :background)
                            :-fx-border-width     0
                            :-fx-padding [5 0 5 0]}}

       "-system-call-item" {:-fx-background-color (colors :background)
                            :-fx-padding          [2 0 2 10]
                            :-fx-font-size        14
                            :-fx-font-family      "Inconsolata"
                            ":hover"    {:-fx-background-color (colors :sidebar)}
                            "-selected" {:-fx-background-color (colors :sidebar)
                                         :-fx-padding          [2 0 2 10]
                                         :-fx-font-size        14
                                         :-fx-font-family      "Inconsolata"}}

       "-std-out-item" {:-fx-background-color (colors :background)
                        :-fx-padding          [2 0 2 10]
                        :-fx-font-size        14
                        :-fx-font-family      "Inconsolata"}

       "-email" {:-fx-background-color "transparent"
                 :-fx-padding          0
                 :-fx-border-color     (colors :sidebar)
                 :-fx-border-width     [1 0 1 0]

                 "-view" {:-fx-padding      10
                          :-fx-border-width [0 0 1 0]
                          :-fx-border-color (colors :border)
                          :-fx-font-size    15
                          ":hover"    {:-fx-background-color (colors :selection)}

                          "-topic"    {:-fx-border-width [0 0 1 0]
                                       :-fx-padding      [0 0 10 0]
                                       :-fx-border-color (colors :border)}

                          "-selected" {:-fx-background-color (colors :selection)}

                          "-meta"     {:-fx-text-fill (colors :meta)
                                       :-fx-font-size 11
                                       :-fx-padding   [10 0 10 0]}

                          "-body"     {:-fx-padding     0
                                       :-fx-font-family "Inconsolata"}

                          "-offest-button" {:-fx-alignment "CENTER"
                                            :-fx-font-size 10}}}}})))
