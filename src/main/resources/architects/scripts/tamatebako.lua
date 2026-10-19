return {
  on_equip = function(ctx)
    ctx.set_state("last_combat", ctx.tick)
    ctx.set_state("ready", 0)
  end,
  on_hit = function(ctx)
    if ctx.damage_kind == "SECONDARY" or ctx.damage_kind == "ENVIRONMENT" then return end
    ctx.set_state("last_combat", ctx.tick)
  end,
  on_hurt = function(ctx)
    if ctx.damage_kind == "SECONDARY" or ctx.damage_kind == "ENVIRONMENT" then return end
    ctx.set_state("last_combat", ctx.tick)
  end,
  on_tick = function(ctx)
    if ctx.tick < ctx.state("last_combat", ctx.tick) + ctx.params.quiet_ticks then return end
    if ctx.tick < ctx.state("ready", 0) or ctx.health >= ctx.max_health then return end
    if ctx.heal(ctx.params.heal) then
      ctx.set_state("ready", ctx.tick + ctx.params.interval)
      ctx.effect("heal")
    end
  end,
  on_unequip = function(ctx)
    ctx.set_state("last_combat", nil)
    ctx.set_state("ready", nil)
  end
}
