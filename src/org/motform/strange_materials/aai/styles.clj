(ns org.motform.strange-materials.aai.styles
  (:require [cljfx.css :as css]))

(def ^:private colors
  {:background "hsb(0, 0%, 100%)"
   :scroll-bar "hsb(0, 0%, 100%)"
   :sidebar    "hsb(0, 0%, 90%)"
   :highlight  "hsb(0, 0%, 70%)"
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
   {".root" {:-fx-background-color (colors :background)
             :-fx-font-family      "Inconsolata"}

    ".scroll-bar:vertical"   {:-fx-pref-width  10}
    ".scroll-bar:horizontal" {:-fx-pref-height 10}

    ".track" {:-fx-background-color (colors :frame)}
    ".thumb" {:-fx-background-color (colors :scroll-bar) :-fx-background-radius 0}

    ".increment-button" {:-fx-background-color (colors :frame) :-fx-padding 0   :-fx-border-color (colors :frame) }
    ".decrement-button" {:-fx-background-color (colors :frame) :-fx-padding -50 :-fx-border-color (colors :frame)}
    ".decrement-arrow"  {:-fx-padding 0 :-fx-border-width 0 :-fx-border-color (colors :frame) :-fx-shape " "}
    ".increment-arrow"  {:-fx-padding 0 :-fx-border-width 0 :-fx-border-color (colors :frame) :-fx-shape " "}
    ".scroll-arrow"     {:-fx-padding 0}

    ".text-field" {:-fx-background-color  (colors :background)
                   :-fx-text-fill         (colors :foreground)
                   :-fx-padding           [15 15 15 15]
                   :-fx-font-family       "Inconsolata"
                   :-fx-font-size         25
                   :-fx-background-radius 0}

    ".pane" {:-fx-font-size 18}

    ".chat" {"-input" {"-label" {:-fx-padding   [0 0 10 0]
                                 :-fx-font-size 13
                                 :-fx-text-fill (colors :meta)}

                       "-container" {:-fx-padding          [15 10 15 20]
                                     :-fx-border-width     [0 0 1 0]
                                     :-fx-border-color     (colors :border)
                                     :-fx-background-color (colors :selection)}}

             "-smart-reply" {:-fx-padding           [5 10] 
                             :-fx-border-width      1
                             :-fx-border-radius     5
                             :-fx-font-size         15
                             :-fx-background-color  (colors :selection)
                             :-fx-background-radius 5
                             :-fx-border-color      (colors :border)
                             ":hover" {:-fx-background-color  (colors :sidebar)
                                       :-fx-background-radius 5}

                             "-loading" {:-fx-padding [6 11]}

                             "-container" {:-fx-padding 20
                                           :-fx-spacing 20
                                           :-fx-border-width     [1 0 0 0]
                                           :-fx-border-color     (colors :border)}}

             "-messages" {:-fx-padding [10 25]
                          "-message" {:-fx-padding          [5 10] 
                                      :-fx-border-width     1
                                      :-fx-border-radius    5
                                      :-fx-font-size        15
                                      :-fx-background-color (colors :selection)
                                      :-fx-border-color     (colors :border)
                                      "-container" {:-fx-padding [5 0]}}}}

    ".server" {"-status" {:-fx-padding      10
                          :-fx-spacing      10
                          :-fx-border-width [0 0 1 0]
                          :-fx-border-color (colors :border)
                          :-fx-background-color (colors :selection)

                          "-channel" {:-fx-padding       5
                                      :-fx-font-size     12
                                      :-fx-text-fill     (colors :meta)
                                      :-fx-font-family   "Inconsolata"}

                          "-connect" {:-fx-padding       5
                                      :-fx-font-size     12
                                      :-fx-text-fill     (colors :border)
                                      :-fx-font-family   "Inconsolata"
                                      ":hover" {:-fx-background-color (colors :border)
                                                :-fx-text-fill        (colors :foreground)}}}

               "-chat" {"-view" {:-fx-spacing 10
                                 :-fx-padding 10

                                 "-sender" {:-fx-font-size   14
                                            :-fx-padding     5
                                            :-fx-font-family "Inconsolata"
                                            :-fx-text-fill   (colors :meta)}

                                 "-message" {:-fx-padding          [5 10]
                                             :-fx-border-width     1
                                             :-fx-border-radius    5
                                             :-fx-font-size        15
                                             :-fx-font-family      "Inconsolata"
                                             :-fx-background-color (colors :selection)
                                             :-fx-border-color     (colors :border)
                                             "-container" {:-fx-padding [5 0]}}}

                        "-panel" {:-fx-spacing 20}}

               "-interception" {"-container" {:-fx-spacing      [10 20]
                                              :-fx-border-width [0 0 0 1]
                                              :-fx-border-color (colors :border)}

                                "-section" {:-fx-padding [0 0 20 0]
                                            :-fx-spacing 10}

                                "-editor" {:-fx-spacing 10
                                           :-fx-padding 10

                                           "-completion" {:-fx-padding       10
                                                          :-fx-border-width  1
                                                          :-fx-border-radius 5
                                                          :-fx-border-color  (colors :border)
                                                          :-fx-font-size     18
                                                          :-fx-font-family   "Inconsolata"}

                                           "-prompt" {"-static" {:-fx-font-family   "Inconsolata"
                                                                 :-fx-padding       10
                                                                 :-fx-border-width  1
                                                                 :-fx-border-radius 5
                                                                 :-fx-border-color  (colors :border)}

                                                      "-editable" {:-fx-font-family "Inconsolata"

                                                                   "-container" {:-fx-border-width     1
                                                                                 :-fx-padding          10
                                                                                 :-fx-border-radius    5
                                                                                 :-fx-border-color     (colors :border)
                                                                                 :-fx-background-color (colors :selection)}}}

                                           "-label" {:-fx-padding     [5 0 2 0]
                                                     :-fx-font-size   15
                                                     :-fx-font-family "Inconsolata"
                                                     :-fx-text-fill   (colors :meta)}

                                           "-submit" {:-fx-padding          10
                                                      :-fx-border-width     1
                                                      :-fx-border-radius    5
                                                      :-fx-font-size   15
                                                      :-fx-background-color (colors :selection)
                                                      :-fx-font-family      "Inconsolata"
                                                      :-fx-border-color     (colors :border)

                                                      ":hover"
                                                      {:-fx-background-radius 5
                                                       :-fx-background-color (colors :sidebar)}}}}}}))


