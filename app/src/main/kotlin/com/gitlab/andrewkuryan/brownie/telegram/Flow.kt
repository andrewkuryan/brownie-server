package com.gitlab.andrewkuryan.brownie.telegram

import arrow.core.Option
import arrow.core.Some
import arrow.core.toOption
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface TriggerMessage {
    val text: String
}

interface PromptTextHolder<in T : Any> {
    val promptText: suspend (T) -> String
}

sealed class FlowVertex {
    sealed interface NonRoot<in T : Any>
    sealed interface NonLeaf

    data class Root(val name: String) : FlowVertex(), NonLeaf
    data class Internal<in T : Any>(override val promptText: suspend (T) -> String) :
        FlowVertex(), PromptTextHolder<T>, NonRoot<T>, NonLeaf

    data class Decision<in T : Any, out D : Any>(val determiner: suspend (T) -> D) : FlowVertex(), NonRoot<T>, NonLeaf
    data class Leaf<in T : Any>(override val promptText: suspend (T) -> String) :
        FlowVertex(), PromptTextHolder<T>, NonRoot<T>
}

sealed class FlowEdge<T : Any> {
    abstract val to: FlowVertex.NonRoot<T>
    abstract val from: FlowVertex.NonLeaf

    sealed class Executable<T : Any> : FlowEdge<T>() {
        abstract val action: (suspend (Message) -> Option<T>)?
    }

    data class Trigger<in F : Any, T : Any, TR : TriggerMessage>(
        override val from: FlowVertex.Internal<F>,
        override val to: FlowVertex.NonRoot<T>,
        val trigger: TR,
        override val action: (suspend (Message) -> Option<T>)? = null,
    ) : Executable<T>()

    data class Command<T : Any>(
        override val from: FlowVertex.NonLeaf,
        override val to: FlowVertex.NonRoot<T>,
        val command: BotCommand,
        override val action: (suspend (Message) -> Option<T>)? = null,
    ) : Executable<T>()

    data class AnyText<in F : Any, T : Any>(
        override val from: FlowVertex.Internal<F>,
        override val to: FlowVertex.NonRoot<T>,
        override val action: (suspend (Message) -> Option<T>)? = null,
    ) : Executable<T>()

    data class AnyMessage<in F : Any, T : Any>(
        override val from: FlowVertex.Internal<F>,
        override val to: FlowVertex.NonRoot<T>,
        override val action: (suspend (Message) -> Option<T>)? = null,
    ) : Executable<T>()

    data class Decision<in F : Any, D : Any, T : Any>(
        override val from: FlowVertex.Decision<F, D>,
        override val to: FlowVertex.NonRoot<T>,
        val determiner: suspend (D) -> Boolean,
        val action: (suspend (F) -> Option<T>)? = null,
    ) : FlowEdge<T>()
}

class FlowContext<TR> where TR : Enum<*>, TR : TriggerMessage {

    object EmptyVertex : FlowVertex(), FlowVertex.NonRoot<Unit>, FlowVertex.NonLeaf

    private var edges = mutableListOf<FlowEdge<*>>()
    private lateinit var determiner: suspend (Message) -> FlowVertex.NonLeaf

    fun determineVertex(determiner: suspend (Message) -> FlowVertex.NonLeaf) {
        this.determiner = determiner
    }

    infix fun <F : Any> FlowVertex.Internal<F>.byTrigger(trigger: TR): FlowEdge.Trigger<F, Unit, TR> =
        FlowEdge.Trigger(from = this, trigger = trigger, to = EmptyVertex).apply { edges.add(this) }

    fun FlowVertex.NonLeaf.byCommand(command: BotCommand): FlowEdge.Command<Unit> =
        FlowEdge.Command(from = this, command = command, to = EmptyVertex).apply { edges.add(this) }

    fun <F : Any> FlowVertex.Internal<F>.byAnyText(): FlowEdge.AnyText<F, Unit> =
        FlowEdge.AnyText(from = this, to = EmptyVertex).apply { edges.add(this) }

    fun <F : Any> FlowVertex.Internal<F>.byAnyMessage(): FlowEdge.AnyMessage<F, Unit> =
        FlowEdge.AnyMessage(from = this, to = EmptyVertex).apply { edges.add(this) }

    infix fun <F : Any, D : Any> FlowVertex.Decision<F, D>.byDecision(determiner: suspend (D) -> Boolean): FlowEdge.Decision<F, D, Unit> =
        FlowEdge.Decision(from = this, determiner = determiner, to = EmptyVertex).apply { edges.add(this) }

    infix fun <F : Any, D : Any, T : Any> FlowEdge.Decision<F, D, *>.toVertex(vertex: FlowVertex.NonRoot<T>): FlowEdge.Decision<F, D, T> =
        FlowEdge.Decision(from = from, determiner = determiner, to = vertex)
            .apply { edges.remove(this@toVertex); edges.add(this) }

    infix fun <F : Any, D : Any, T : Any> FlowEdge.Decision<F, D, T>.withAction(action: suspend (F) -> Option<T>): FlowEdge.Decision<F, D, T> =
        FlowEdge.Decision(from = from, determiner = determiner, to = to, action = action)
            .apply { edges.remove(this@withAction); edges.add(this) }

    infix fun <T : Any> FlowEdge.Executable<*>.toVertex(vertex: FlowVertex.NonRoot<T>): FlowEdge.Executable<T> =
        when (this) {
            is FlowEdge.Trigger<*, *, *> -> FlowEdge.Trigger(from = from, trigger = trigger, to = vertex)
            is FlowEdge.Command<*> -> FlowEdge.Command(from = from, command = command, to = vertex)
            is FlowEdge.AnyText<*, *> -> FlowEdge.AnyText(from = from, to = vertex)
            is FlowEdge.AnyMessage<*, *> -> FlowEdge.AnyMessage(from = from, to = vertex)
        }.apply { edges.remove(this@toVertex); edges.add(this) }

    infix fun <T : Any> FlowEdge.Executable<T>.withAction(action: suspend (Message) -> Option<T>): FlowEdge.Executable<T> =
        when (this) {
            is FlowEdge.Trigger<*, *, *> -> FlowEdge.Trigger(from = from, trigger = trigger, to = to, action = action)
            is FlowEdge.Command<*> -> FlowEdge.Command(from = from, command = command, to = to, action = action)
            is FlowEdge.AnyText<*, *> -> FlowEdge.AnyText(from = from, to = to, action = action)
            is FlowEdge.AnyMessage<*, *> -> FlowEdge.AnyMessage(from = from, to = to, action = action)
        }.apply { edges.remove(this@withAction); edges.add(this) }

    private suspend fun <T : Any, D : Any> processDecisionVertex(
        vertex: FlowVertex.Decision<T, D>,
        message: Message,
        getBot: () -> Bot,
        actionResult: T,
    ) {
        val decision = vertex.determiner(actionResult)
        edges.filter { it.from == vertex }.filterIsInstance<FlowEdge.Decision<T, D, *>>()
            .findLast { it.determiner(decision) }?.let { decisionEdge ->
                moveByDecisionEdge(message, getBot, actionResult, decisionEdge)
            }
    }

    private suspend fun <T : Any> processPromptTextVertex(
        vertex: PromptTextHolder<T>,
        message: Message,
        getBot: () -> Bot,
        actionResult: T,
    ) {
        val outgoingEdges = edges.filter { it.from == vertex }
        val outgoingTriggers = outgoingEdges.filterIsInstance<FlowEdge.Trigger<*, *, *>>()
        getBot().sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = @Suppress("UNCHECKED_CAST") (vertex.promptText as suspend (Any) -> String)(actionResult),
            replyMarkup = if (outgoingTriggers.isNotEmpty()) KeyboardReplyMarkup(
                keyboard = outgoingTriggers.map { listOf(KeyboardButton(it.trigger.text)) },
                resizeKeyboard = true,
            ) else ReplyKeyboardRemove(removeKeyboard = true)
        )
    }

    private suspend fun <T : Any> processVertex(
        message: Message,
        getBot: () -> Bot,
        actionResult: T,
        vertex: FlowVertex.NonRoot<T>
    ) {
        when (vertex) {
            is FlowVertex.Internal -> processPromptTextVertex(vertex, message, getBot, actionResult)
            is FlowVertex.Leaf -> processPromptTextVertex(vertex, message, getBot, actionResult)
            is FlowVertex.Decision<T, *> -> processDecisionVertex(vertex, message, getBot, actionResult)
            is EmptyVertex -> throw Exception("Vertex is not initialized: $vertex")
        }
    }

    private suspend fun <T : Any> moveByDecisionEdge(
        message: Message,
        getBot: () -> Bot,
        actionResult: T,
        edge: FlowEdge.Decision<T, *, *>
    ) {
        (edge.action?.let { it(actionResult) } ?: Some(Unit)).tap { result ->
            @Suppress("UNCHECKED_CAST")
            val toVertex = edge.to as FlowVertex.NonRoot<Any>
            processVertex(message, getBot, result, toVertex)
        }
    }

    private suspend fun moveByExecutableEdge(message: Message, getBot: () -> Bot, edge: FlowEdge.Executable<*>) {
        (edge.action?.let { it(message) } ?: Some(Unit)).tap { result ->
            @Suppress("UNCHECKED_CAST")
            val toVertex = edge.to as FlowVertex.NonRoot<Any>
            processVertex(message, getBot, result, toVertex)
        }
    }

    private suspend fun handleMessage(message: Message, getBot: () -> Bot) {
        val vertex = determiner(message)
        val outgoingEdges = edges
            .filterIsInstance<FlowEdge.Executable<*>>().filter { it.from == vertex }
        listOf(
            outgoingEdges.findLast {
                it is FlowEdge.Command<*> && message.text != null && message.text!!.startsWith("/${it.command.command}")
            },
            outgoingEdges.findLast { it is FlowEdge.Trigger<*, *, *> && message.text == it.trigger.text },
            outgoingEdges.findLast { it is FlowEdge.AnyText<*, *> && message.text != null },
            outgoingEdges.findLast { it is FlowEdge.AnyMessage<*, *> }
        ).firstOrNull { it != null }?.let { edgeToMove ->
            moveByExecutableEdge(message, getBot, edgeToMove)
        }
    }

    fun build(dispatcher: Dispatcher, getBot: () -> Bot) {
        dispatcher.apply {
            message {
                CoroutineScope(Dispatchers.IO).launch {
                    handleMessage(message, getBot)
                }
            }
        }
    }
}

fun <M> Dispatcher.flowContext(
    getBot: () -> Bot,
    builder: FlowContext<M>.() -> Unit
): FlowContext<M> where M : Enum<*>, M : TriggerMessage {
    return FlowContext<M>().apply(builder).apply { build(this@flowContext, getBot) }
}

@Suppress("UNCHECKED_CAST")
suspend fun <T1 : Any, T2 : Any, R : Any> withParams(
    fn1: () -> Option<T1>, fn2: suspend (T1) -> Option<T2>,
    body: suspend (T1, T2) -> R?
) = withParams(fn1, listOf(fn2) as List<suspend (Any) -> Option<Any>>)
    .takeIf { it.size == 2 }
    ?.let { body(it[0] as T1, it[1] as T2) }
    .toOption()

@Suppress("UNCHECKED_CAST")
suspend fun <T1 : Any, T2 : Any, T3 : Any, R : Any> withParams(
    fn1: () -> Option<T1>, fn2: suspend (T1) -> Option<T2>, fn3: suspend (T2) -> Option<T3>,
    body: suspend (T1, T2, T3) -> R?
) = withParams(fn1, listOf(fn2, fn3) as List<suspend (Any) -> Option<Any>>)
    .takeIf { it.size == 3 }
    ?.let { body(it[0] as T1, it[1] as T2, it[2] as T3) }
    .toOption()

@Suppress("UNCHECKED_CAST")
suspend fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, R : Any> withParams(
    fn1: () -> Option<T1>,
    fn2: suspend (T1) -> Option<T2>,
    fn3: suspend (T2) -> Option<T3>,
    fn4: suspend (T3) -> Option<T4>,
    body: suspend (T1, T2, T3, T4) -> R?
) = withParams(fn1, listOf(fn2, fn3, fn4) as List<suspend (Any) -> Option<Any>>)
    .takeIf { it.size == 4 }
    ?.let { body(it[0] as T1, it[1] as T2, it[2] as T3, it[3] as T4) }
    .toOption()

private suspend fun withParams(fn1: () -> Option<Any>, fnList: List<suspend (Any) -> Option<Any>>): List<Any> =
    fnList.fold(fn1().fold({ listOf() }, { listOf(it) })) { acc, fn ->
        fn(acc.last()).fold({ acc }, { acc + it })
    }