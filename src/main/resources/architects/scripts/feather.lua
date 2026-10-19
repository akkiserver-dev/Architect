return {
  on_equip = function(ctx)
    ctx.attribute("speed", ctx.params.speed_bonus)
  end,
  on_damage_taken = function(ctx)
    ctx.scale_damage(ctx.params.damage_taken)
  end
}
