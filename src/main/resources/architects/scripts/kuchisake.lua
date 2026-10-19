return {
  on_attack = function(ctx)
    local hits = ctx.cast("cone", ctx.params.left_range, ctx.params.left_half_angle,
      ctx.params.left_damage, "AREA_TOOL", ctx.params.left_knockback)
    if hits > 0 then
      ctx.set_state("ready_until", ctx.tick + ctx.params.ready_ticks)
      ctx.notice("二口: 右クリックで追撃")
    end
    return true
  end,
  on_use = function(ctx)
    if ctx.state("ready_until", 0) <= ctx.tick then
      ctx.notice("二口: 先に左クリックを当てる")
      return false
    end
    ctx.set_state("ready_until", 0)
    ctx.cast("first", ctx.params.right_range, ctx.params.right_width,
      ctx.params.right_damage, "TOOL", ctx.params.right_knockback)
    return true
  end
}
