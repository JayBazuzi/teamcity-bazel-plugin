package bazel.messages.handlers

import bazel.HandlerPriority
import bazel.Verbosity
import bazel.atLeast
import bazel.bazel.events.BazelEvent
import bazel.bazel.events.BuildFinished
import bazel.messages.ServiceMessageContext

class BuildCompletedHandler : EventHandler {
    override val priority: HandlerPriority
        get() = HandlerPriority.Low

    override fun handle(ctx: ServiceMessageContext) =
            if (ctx.event.payload is BazelEvent && ctx.event.payload.content is BuildFinished) {
                val event = ctx.event.payload.content
                if (event.exitCode == 0) {
                    if (ctx.verbosity.atLeast(Verbosity.Detailed)) {
                        ctx.onNext(ctx.messageFactory.createMessage(
                                ctx.buildMessage()
                                        .append("Build finished")
                                        .append(" with exit code ${event.exitCode}")
                                        .append("(${event.exitCodeName})", Verbosity.Verbose)
                                        .toString()))
                    }
                } else {
                    ctx.onNext(ctx.messageFactory.createBuildProblem(
                            ctx.buildMessage(false)
                                    .append("Build failed")
                                    .append(" with exit code ${event.exitCode}", Verbosity.Detailed)
                                    .append(" (${event.exitCodeName})", Verbosity.Verbose)
                                    .toString(),
                            ctx.event.projectId,
                            event.id.toString()))
                }

                true
            } else ctx.handlerIterator.next().handle(ctx)
}