/*
 * Copyright 2014 Thomas Hoffmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kz.evilteamgenius.chessapp.engine;

import android.util.Pair;

public class Player {

    public final String id;
    public final int team;
    public final int color;
    public final String name;
    public Pair<Coordinate, Coordinate> lastMove;

    public Player(final String i, int t, int c, final String n) {
        id = i;
        team = t;
        color = c;
        name = n;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Player && ((Player) other).id.equals(id);
    }
}
