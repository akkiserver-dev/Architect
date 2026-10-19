return {
  on_attack = function(ctx)
    local hits = ctx.cast("cone", ctx.params.range, ctx.params.angle, ctx.params.damage, "AREA_TOOL", 0.1)
    if hits > 0 then
      ctx.set_state("ready_until", ctx.tick + ctx.params.window)
      ctx.notice("白兎: 右クリックで跳べる")
    end
    return true
  end,
  on_use = function(ctx)
    if ctx.state("ready_until", 0) <= ctx.tick then
      ctx.notice("白兎: 一撃を当ててから跳ぶ")
      return false
    end
    if not ctx.dash(ctx.params.speed, ctx.params.lift) then return false end
    ctx.set_state("ready_until", 0)
    ctx.effect("wind")
    return true
  end
}
