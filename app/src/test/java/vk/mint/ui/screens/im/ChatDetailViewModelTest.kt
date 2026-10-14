package vk.mint.ui.screens.im

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vk.mint.data.model.Message
import vk.mint.data.model.MessageReaction
import vk.mint.data.model.ReactionItem

class ChatDetailViewModelTest {

    private fun createMessage(
        id: Long,
        cmid: Long? = null,
        isOut: Boolean = false,
        readState: Int = 0,
        text: String = "Test message",
        reactions: MessageReaction? = null,
    ): Message {
        return Message(
            id = id,
            peerId = 12345L,
            fromId = if (isOut) 1L else 2L,
            date = System.currentTimeMillis() / 1000,
            text = text,
            out = if (isOut) 1 else 0,
            readState = readState,
            conversationMessageId = cmid,
            reactions = reactions,
        )
    }

    @Test
    fun testIsReadUpTo_withCmid() {
        val msgBefore = createMessage(id = 100, cmid = 10)
        val msgExact = createMessage(id = 101, cmid = 20)
        val msgAfter = createMessage(id = 102, cmid = 30)

        assertTrue(ChatDetailViewModel.isReadUpTo(msgBefore, upToCmid = 20))
        assertTrue(ChatDetailViewModel.isReadUpTo(msgExact, upToCmid = 20))
        assertFalse(ChatDetailViewModel.isReadUpTo(msgAfter, upToCmid = 20))
    }

    @Test
    fun testIsReadUpTo_fallbackToId() {
        val msgServer = createMessage(id = 15, cmid = null)
        val msgOptimistic = createMessage(id = -999L, cmid = null)

        assertTrue(ChatDetailViewModel.isReadUpTo(msgServer, upToCmid = 20))
        assertFalse(ChatDetailViewModel.isReadUpTo(msgServer, upToCmid = 10))
        // Отрицательные ID (оптимистичные сообщения) никогда не считаются прочитанными
        assertFalse(ChatDetailViewModel.isReadUpTo(msgOptimistic, upToCmid = 20))
    }

    @Test
    fun testApplyReadState() {
        val outMsg1 = createMessage(id = 1, cmid = 10, isOut = true, readState = 0)
        val outMsg2 = createMessage(id = 2, cmid = 30, isOut = true, readState = 0)
        val inMsg = createMessage(id = 3, cmid = 10, isOut = false, readState = 0)
        val list = listOf(outMsg1, outMsg2, inMsg)

        val updated = ChatDetailViewModel.applyReadState(list, upToCmid = 20, isOut = true)
        assertEquals(1, updated[0].readState)
        assertEquals(0, updated[1].readState)
        assertEquals(0, updated[2].readState)
    }

    @Test
    fun testApplyOptimisticAndRemove() {
        val initial = listOf(createMessage(id = 1), createMessage(id = 2))
        val optimistic = createMessage(id = -100, text = "Sending...")

        val withOptimistic = ChatDetailViewModel.applyOptimisticMessage(initial, optimistic)
        assertEquals(3, withOptimistic.size)
        assertEquals(-100L, withOptimistic[0].id)

        val reverted = ChatDetailViewModel.removeMessageById(withOptimistic, id = -100L)
        assertEquals(2, reverted.size)
        assertEquals(1L, reverted[0].id)
    }

    @Test
    fun testApplyReactionToggle_addNewReaction() {
        val msg = createMessage(id = 1)
        val updatedList = ChatDetailViewModel.applyReactionToggle(
            messages = listOf(msg),
            messageId = 1L,
            reactionId = 16, // Like
            isToggleOff = false,
        )

        val updated = updatedList[0]
        assertEquals(1, updated.reactions?.count)
        assertEquals(16, updated.reactions?.userReaction)
        assertEquals(1, updated.reactions?.items?.size)
        assertEquals(16, updated.reactions?.items?.get(0)?.id)
        assertEquals(1, updated.reactions?.items?.get(0)?.count)
    }

    @Test
    fun testApplyReactionToggle_removeExistingReaction() {
        val existingReaction = MessageReaction(
            count = 1,
            userReaction = 16,
            items = listOf(ReactionItem(id = 16, count = 1)),
        )
        val msg = createMessage(id = 1, reactions = existingReaction)

        val updatedList = ChatDetailViewModel.applyReactionToggle(
            messages = listOf(msg),
            messageId = 1L,
            reactionId = 16,
            isToggleOff = true,
        )

        val updated = updatedList[0]
        assertEquals(0, updated.reactions?.count)
        assertNull(updated.reactions?.userReaction)
        assertTrue(updated.reactions?.items.isNullOrEmpty())
    }
}
