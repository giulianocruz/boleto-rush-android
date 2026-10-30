package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameMagnetTest {
    @Test fun coinsCannotTeleportOutOfHiddenArea() {
        val original = GameMagnet.Position(.09f, .20f)
        val next = GameMagnet.pull(original.x, original.y, .91f, .05f, true)
        assertEquals(original.x,next.x,.0001f)
        assertEquals(original.y,next.y,.0001f)
        assertFalse(GameMagnet.reachedWallet(next.x,next.y,.91f))
    }

    @Test fun disabledMagnetNeverMovesCoins() {
        val next = GameMagnet.pull(.12f,.50f,.85f,.05f,false)
        assertEquals(.12f,next.x,.0001f)
        assertEquals(.50f,next.y,.0001f)
    }

    @Test fun magnetPullsTowardsWalletGraduallyAndEarnsOnlyOnArrival() {
        var x = .1f
        var y = .42f
        val wallet = .85f
        var arrived = false
        for (frame in 1..80) {
            val next = GameMagnet.pull(x,y,wallet,.016f,true)
            assertTrue(next.x > x || frame > 1 && x >= wallet)
            assertTrue(next.y >= y)
            assertTrue(next.x in .09f..91f)
            assertTrue("A visible coin must never jump directly to wallet",
                kotlin.math.abs(next.x - x) < .15f)
            x = next.x
            y = next.y
            if (GameMagnet.reachedWallet(x,y,wallet)) {
                assertTrue("Coin collected without reaching visible area", y >= .755f)
                arrived = true
                break
            }
        }
        assertTrue("Coin never reached wallet",arrived)
    }

    @Test fun offscreenAndFarAwayCoinsNeverScore() {
        assertFalse(GameMagnet.reachedWallet(.20f,.46f,.85f))
        assertFalse(GameMagnet.reachedWallet(.70f,.82f,.85f))
        assertFalse(GameMagnet.reachedWallet(.85f,.95f,.85f))
        assertTrue(GameMagnet.reachedWallet(.85f,.82f,.85f))
    }

    @Test fun pauseOrZeroDeltaCannotTeleportCoins() {
        val p = GameMagnet.pull(.18f,.50f,.84f,0f,true)
        assertEquals(.18f,p.x,.0001f)
        assertEquals(.50f,p.y,.0001f)
    }
}
