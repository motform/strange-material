(ns motform.strange.material.styles
  (:require [cljfx.css :as css]))

(def ^:private colors
  {:background "hsb(0, 0%, 100%)"
   :scroll-bar "hsb(0, 0%, 95%)"
   :sidebar    "hsb(0, 0%, 80%)"

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

      "@font-face"
      {:src "url(\"resources/fonts/MDSystemTest-Regular.otf\")"}

      ".root"
      {:-fx-background-color (colors :background)
       :-fx-text-fill        (colors :foreground)
       :-fx-font-size        20}

      ".sidebar"
      {:-fx-background-color (colors :sidebar)}

      ".sidebar-panel-indicator"
      {:-fx-text-fill      (colors :inactive)
       :-fx-alignment      "CENTER"
       :-fx-padding        [7 15 7 15]
       ":hover"
       {:-fx-text-fill        (colors :foreground)
        :-fx-background-color (colors :border)}}

      ".sidebar-panel-indicator-active"
      {:-fx-text-fill        (colors :foreground)
       :-fx-background-color (colors :background)
       ":hover"
       {:-fx-background-color (colors :background)}}

      ".panel"
      {:-fx-padding 0
       :-fx-spacing 0}

      ".timeline"
      {}

      ".repl"
      {:-fx-font-family "Inconsolata"
       :-fx-font-size   20}

      ".repl-submit"
      {:-fx-padding           10
       :-fx-border-width      1
       :-fx-font-size         15
       :-fx-background-color  (colors :sidebar)}

      ".repl-tape"
      {:-fx-font-family       "Inconsolata"
       :-fx-background-color  (colors :sidebar)
       :-fx-padding           20
       :-fx-font-size         20}

      ;;; COMMAND LINE

      ".text-field"
      {:-fx-background-color (colors :background)
       :-fx-background-radius 0}

      ".kernel-command-line"
      {:-fx-padding [0 0 0 0]
       :-fx-background (colors :sidebar)}

      ;;; SIDEBAR

      ".kernel-sidebar"
      {:-fx-font-size    12
       :-fx-border-color (colors :sidebar)
       :-fx-border-width [1 0 1 1]}

      ".kernel-sidebar-container"
      {}

      ".kernel-sidebar-container-label"
      {:-fx-padding          [5 5 5 10]
       :-fx-font-size        12
       :-fx-font-weight      "bold"
       :-fx-background-color (colors :sidebar)}



      ;;; SCROLL BAR STYLES 


      ".scroll-bar:vertical"   {:-fx-pref-width  10}
      ".scroll-bar:horizontal" {:-fx-pref-height 10}

      ".track" {:-fx-background-color (colors :sidebar)}
      ".thumb" {:-fx-background-color (colors :scroll-bar) :-fx-background-radius 0}

      ".increment-button" {:-fx-background-color (colors :sidebar) :-fx-padding 0 :-fx-border-color (colors :sidebar) }
      ".decrement-button" {:-fx-background-color (colors :sidebar) :-fx-padding -50 :-fx-border-color (colors :sidebar)}
      ".decrement-arrow"  {:-fx-padding 0 :-fx-border-width 0 :-fx-border-color (colors :sidebar) :-fx-shape " "}
      ".increment-arrow"  {:-fx-padding 0 :-fx-border-width 0 :-fx-border-color (colors :sidebar) :-fx-shape " "}
      ".scroll-arrow"     {:-fx-padding 0}


      ;;; SYSTEM CALLS


      ".kernel-sidebar-list"
      {:-fx-background-color (colors :background)
       :-fx-border-width     0
       :-fx-padding [5 0 5 0]}

      ".kernel-system-call-item"
      {:-fx-background-color (colors :background)
       :-fx-padding          [2 0 2 10]
       :-fx-font-size        14
       :-fx-font-family      "Inconsolata"}

      ".kernel-system-call-item:hover"
      {:-fx-background-color  (colors :sidebar)}

      ".kernel-system-call-item-selected"
      {:-fx-background-color (colors :sidebar)
       :-fx-padding          [2 0 2 10]
       :-fx-font-size        14
       :-fx-font-family      "Inconsolata"}

      ".kernel-std-out-item"
      {:-fx-background-color (colors :background)
       :-fx-padding          [2 0 2 10]
       :-fx-font-size        14
       :-fx-font-family      "Inconsolata"}

      ;;; EMAILS

      ".kernel-emails"
      {:-fx-background-color "transparent"
       :-fx-padding 0
       :-fx-border-color (colors :sidebar)
       :-fx-border-width [1 0 1 0]}

      ".kernel-email-view"
      {:-fx-padding      10
       :-fx-border-width [0 0 1 0]
       :-fx-border-color (colors :border)
       :-fx-font-size    15
       ":hover"
       {:-fx-background-color (colors :selection)}}

      ".kernel-email-view-topic"
      {:-fx-border-width [0 0 1 0]
       :-fx-padding      [0 0 10 0]
       :-fx-border-color (colors :border)}

      ".kernel-email-view-selected"
      {:-fx-background-color (colors :selection)}

      ".kernel-email-view-meta"
      {:-fx-text-fill (colors :meta)
       :-fx-font-size 11
       :-fx-padding   [10 0 10 0]}

      ".kernel-email-view-body"
      {:-fx-padding     0
       :-fx-font-family "Inconsolata"}

      "kernel-email-offset-button"
      {:-fx-alignment "CENTER"
       :-fx-font-size 10}
      })))

