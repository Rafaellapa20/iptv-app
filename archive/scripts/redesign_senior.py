# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_senior_main.xml', 'w', encoding='utf-8') as f:
    f.write('''<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#0A0A14">

    <!-- Header -->
    <LinearLayout
        android:id="@+id/llHeaderSenior"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentTop="true"
        android:orientation="horizontal"
        android:padding="32dp"
        android:gravity="center_vertical">

        <ImageView
            android:layout_width="60dp"
            android:layout_height="60dp"
            android:src="@drawable/logo"
            android:scaleType="fitCenter"
            android:layout_marginEnd="16dp"/>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Modo Fácil"
            android:textColor="#FFFFFF"
            android:textSize="28sp"
            android:textStyle="bold" />
            
        <View
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:layout_weight="1" />
            
        <TextView
            android:id="@+id/tvClockSenior"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textColor="#FFFFFF"
            android:textSize="24sp"
            android:textStyle="bold" />
    </LinearLayout>

    <!-- Grid of Cards -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_below="@id/llHeaderSenior"
        android:layout_above="@id/btnSairSenior"
        android:orientation="vertical"
        android:paddingHorizontal="48dp"
        android:paddingBottom="16dp"
        android:weightSum="2">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:orientation="horizontal"
            android:weightSum="2">

            <androidx.cardview.widget.CardView
                android:id="@+id/btnSeniorTv"
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:layout_margin="16dp"
                app:cardBackgroundColor="#1A237E"
                app:cardCornerRadius="16dp"
                app:cardElevation="8dp"
                android:focusable="true"
                android:clickable="true"
                android:foreground="?attr/selectableItemBackground"
                android:stateListAnimator="@animator/selector_item_scale">
                
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:gravity="center"
                    android:orientation="vertical"
                    android:background="@drawable/bg_gradient_card_overlay">
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="📺"
                        android:textSize="60sp"
                        android:layout_marginBottom="8dp"/>
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Televisão"
                        android:textColor="#FFFFFF"
                        android:textSize="32sp"
                        android:textStyle="bold" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <androidx.cardview.widget.CardView
                android:id="@+id/btnSeniorFilmes"
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:layout_margin="16dp"
                app:cardBackgroundColor="#004D40"
                app:cardCornerRadius="16dp"
                app:cardElevation="8dp"
                android:focusable="true"
                android:clickable="true"
                android:foreground="?attr/selectableItemBackground"
                android:stateListAnimator="@animator/selector_item_scale">
                
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:gravity="center"
                    android:orientation="vertical"
                    android:background="@drawable/bg_gradient_card_overlay">
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="🎬"
                        android:textSize="60sp"
                        android:layout_marginBottom="8dp"/>
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Filmes"
                        android:textColor="#FFFFFF"
                        android:textSize="32sp"
                        android:textStyle="bold" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:orientation="horizontal"
            android:weightSum="2">

            <androidx.cardview.widget.CardView
                android:id="@+id/btnSeniorSeries"
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:layout_margin="16dp"
                app:cardBackgroundColor="#b71c1c"
                app:cardCornerRadius="16dp"
                app:cardElevation="8dp"
                android:focusable="true"
                android:clickable="true"
                android:foreground="?attr/selectableItemBackground"
                android:stateListAnimator="@animator/selector_item_scale">
                
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:gravity="center"
                    android:orientation="vertical"
                    android:background="@drawable/bg_gradient_card_overlay">
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="📽️"
                        android:textSize="60sp"
                        android:layout_marginBottom="8dp"/>
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Séries"
                        android:textColor="#FFFFFF"
                        android:textSize="32sp"
                        android:textStyle="bold" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <androidx.cardview.widget.CardView
                android:id="@+id/btnSeniorFavoritos"
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:layout_margin="16dp"
                app:cardBackgroundColor="#F57F17"
                app:cardCornerRadius="16dp"
                app:cardElevation="8dp"
                android:focusable="true"
                android:clickable="true"
                android:foreground="?attr/selectableItemBackground"
                android:stateListAnimator="@animator/selector_item_scale">
                
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:gravity="center"
                    android:orientation="vertical"
                    android:background="@drawable/bg_gradient_card_overlay">
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="⭐"
                        android:textSize="60sp"
                        android:layout_marginBottom="8dp"/>
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Favoritos"
                        android:textColor="#FFFFFF"
                        android:textSize="32sp"
                        android:textStyle="bold" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>
        </LinearLayout>
    </LinearLayout>

    <Button
        android:id="@+id/btnSairSenior"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true"
        android:layout_centerHorizontal="true"
        android:layout_marginBottom="24dp"
        android:text="Sair do Modo Fácil"
        android:textColor="#AAAAAA"
        android:background="@android:color/transparent"
        android:textSize="16sp"
        android:focusable="true"
        android:foreground="?attr/selectableItemBackground"
        android:stateListAnimator="@animator/selector_item_scale" />

</RelativeLayout>''')

print("Redesigned Senior Mode XML")
