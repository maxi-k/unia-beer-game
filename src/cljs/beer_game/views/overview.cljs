(ns beer-game.views.overview
  (:require [re-frame.core :as rf]
            [soda-ash.core :as sa]
            [beer-game.api :as api]
            [beer-game.config :as config]))

(defn overview-panel []
  (let [user (rf/subscribe [:user])
        role (:user/role @user)
        img (config/user-role->image role)]
    [sa/Container {:class-name "game-wrapper"
                   :text true}
     [sa/Header {:class-name "role-title"
                 :content (config/user-role->title role)
                 :text-align :center
                 :as :h1
                 :image img}]]))
