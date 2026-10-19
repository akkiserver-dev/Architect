-- Small conditional composition; health mutation and lifecycle checks remain in Java.
return {
  on_hit = function(ctx)
    if ctx.damage_kind == "SECONDARY" then return end
    if ctx.tick < ctx.state("ready", 0) then return end
    ctx.set_state("ready", ctx.tick + ctx.params.interval)
    ctx.heal(ctx.params.heal)
  end
}
