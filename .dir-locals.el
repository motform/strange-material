;;; Directory Local Variables
;;; For more information see (info "(emacs) Directory Variables")

((clojure-mode . ((eval . (define-key evil-normal-state-map (kbd "ö")
                            '(lambda ()
                               (interactive)
                               (cider-interactive-eval "(swap! motform.strange.material.core/*state identity)")))))))
