return {
  on_damage_dealt = function(ctx)
    if ctx.damage_kind == "SECONDARY" or ctx.damage_kind == "ENVIRONMENT" then return end
    if ctx.health <= ctx.max_health * ctx.params.threshold then
      ctx.scale_damage(ctx.params.damage_bonus)
    end
  end,
  on_damage_taken = function(ctx)
    ctx.scale_damage(ctx.params.damage_taken)
  end
}
