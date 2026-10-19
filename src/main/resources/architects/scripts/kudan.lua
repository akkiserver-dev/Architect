return {
  on_equip = function(ctx)
    ctx.set_state("ready", 0)
  end,
  on_tick = function(ctx)
    if ctx.health > ctx.max_health * ctx.params.threshold then return end
    if ctx.tick < ctx.state("ready", 0) then return end
    ctx.set_state("ready", ctx.tick + ctx.params.interval)
    ctx.buff("speed", ctx.params.speed_ticks, ctx.params.speed_amplifier)
    ctx.buff("resistance", ctx.params.resistance_ticks, ctx.params.resistance_amplifier)
    ctx.notice("件・凶兆: 今こそ退け")
  end,
  on_unequip = function(ctx)
    ctx.set_state("ready", nil)
  end
}
