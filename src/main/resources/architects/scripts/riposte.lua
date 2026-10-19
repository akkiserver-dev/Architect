return {
  on_hurt = function(ctx)
    if ctx.damage_kind == "SECONDARY" or ctx.damage_kind == "ENVIRONMENT" then return end
    ctx.set_state("ready_until", ctx.tick + ctx.params.window)
    ctx.notice("八咫鏡・返照: 次の一撃を強化")
  end,
  on_damage_dealt = function(ctx)
    if ctx.damage_kind == "SECONDARY" then return end
    if ctx.state("ready_until", 0) <= ctx.tick then return end
    ctx.set_state("ready_until", 0)
    ctx.scale_damage(ctx.params.multiplier)
  end
}
