return {
  on_attack = function(ctx)
    local hits = ctx.cast("first", ctx.params.range, ctx.params.width, ctx.params.damage, "TOOL", 0.1)
    if hits > 0 then
      local before = ctx.state("fangs", 0)
      local count = math.min(before + 1, ctx.params.max_fangs)
      ctx.set_state("fangs", count)
      if count > before then ctx.notice("蛇骨: 牙を蓄えた") end
    end
    return true
  end,
  on_use = function(ctx)
    local count = ctx.state("fangs", 0)
    if count <= 0 then
      ctx.notice("蛇骨: 突きを当てて牙を蓄える")
      return false
    end
    ctx.set_state("fangs", 0)
    ctx.cast("sphere", ctx.params.burst_radius, 0,
      ctx.params.burst_base + count * ctx.params.burst_per_fang, "AREA_TOOL", ctx.params.knockback)
    ctx.effect("arcane")
    return true
  end
}
