local function clear_hits(ctx)
  ctx.set_state("hit_count", 0)
  ctx.set_state("first_hit", 0)
  ctx.set_state("second_hit", 0)
end

local function expire_hits(ctx)
  local count = ctx.state("hit_count", 0)
  if count == 0 or ctx.tick - ctx.state("first_hit", 0) < ctx.params.window_ticks then
    return count
  end
  local second = ctx.state("second_hit", 0)
  if count == 2 and ctx.tick - second < ctx.params.window_ticks then
    ctx.set_state("first_hit", second)
    ctx.set_state("second_hit", 0)
    ctx.set_state("hit_count", 1)
    return 1
  end
  clear_hits(ctx)
  return 0
end

local function protect(ctx)
  clear_hits(ctx)
  ctx.set_state("ready", ctx.tick + ctx.params.interval)
  ctx.buff("resistance", ctx.params.resistance_ticks, ctx.params.resistance_amplifier)
  ctx.buff("speed", ctx.params.speed_ticks, ctx.params.speed_amplifier)
  ctx.notice("護り牙: 退く隙を得た")
end

return {
  on_equip = function(ctx)
    clear_hits(ctx)
    ctx.set_state("ready", 0)
  end,
  on_hurt = function(ctx)
    if ctx.damage_kind ~= "TOOL" and ctx.damage_kind ~= "AREA_TOOL" then return end
    if ctx.tick < ctx.state("ready", 0) then return end
    local count = expire_hits(ctx)
    if count == 0 then
      if ctx.params.required_hits <= 1 then
        protect(ctx)
      else
        ctx.set_state("first_hit", ctx.tick)
        ctx.set_state("hit_count", 1)
      end
    elseif count == 1 then
      if ctx.params.required_hits <= 2 then
        protect(ctx)
      else
        ctx.set_state("second_hit", ctx.tick)
        ctx.set_state("hit_count", 2)
      end
    else
      protect(ctx)
    end
  end,
  on_tick = function(ctx)
    expire_hits(ctx)
  end,
  on_unequip = function(ctx)
    clear_hits(ctx)
    ctx.set_state("ready", nil)
  end
}
