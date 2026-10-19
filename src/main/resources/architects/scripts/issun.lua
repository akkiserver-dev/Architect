return {
  on_equip = function(ctx)
    ctx.attribute("health", ctx.params.health_penalty)
    ctx.set_state("last_hit", ctx.tick)
  end,
  on_damage_dealt = function(ctx)
    if ctx.damage_kind == "SECONDARY" or ctx.damage_kind == "ENVIRONMENT" then return end
    if ctx.tick >= ctx.state("last_hit", ctx.tick) + ctx.params.window then
      ctx.scale_damage(ctx.params.multiplier)
    end
  end,
  on_hit = function(ctx)
    if ctx.damage_kind == "SECONDARY" or ctx.damage_kind == "ENVIRONMENT" then return end
    ctx.set_state("last_hit", ctx.tick)
  end,
  on_unequip = function(ctx)
    ctx.set_state("last_hit", nil)
  end
}
