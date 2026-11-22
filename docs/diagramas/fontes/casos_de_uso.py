"""Gera o diagrama de casos de uso (UML) em SVG para o projeto Raízes do Nordeste."""
import math

W, H = 1500, 1180
BX0, BY0, BX1, BY1 = 330, 60, 1170, 1000  # fronteira do sistema
COL_L, COL_R = 560, 940
EW, EH = 300, 54  # largura/altura das elipses
TOP, STEP = 140, 84

uc = {}
left = [
    ("UC05", "Cadastrar-se na fidelidade"),
    ("UC06", "Gerenciar consentimentos"),
    ("UC07", "Solicitar eliminação dos dados"),
    ("UC03", "Resgatar pontos"),
    ("UC04", "Acompanhar status do pedido"),
    ("UC01", "Consultar cardápio da unidade"),
    ("UC02", "Realizar pedido"),
    ("UC08", "Consultar fila de pedidos"),
    ("UC09", "Atualizar status do pedido"),
]
right = [
    ("UC19", "Consultar trilha de auditoria"),
    ("UC18", "Manter usuários"),
    ("UC17", "Manter catálogo e unidades"),
    ("UC16", "Consultar relatórios"),
    ("UC15", "Controlar estoque"),
    ("UC14", "Manter cardápio da unidade"),
    ("UC13", "Aplicar desconto"),
]
for i, (k, t) in enumerate(left):
    uc[k] = (COL_L, TOP + i * STEP, t)
for i, (k, t) in enumerate(right):
    uc[k] = (COL_R, TOP + i * STEP, t)
uc["UC12"] = (COL_R, 770, "Cancelar pedido")
uc["UC10"] = (COL_L, 920, "Solicitar pagamento")
uc["UC11"] = (COL_R, 920, "Receber confirmação de pagamento")

actors = {
    "Cliente": (150, 380),
    "Atendente": (150, 750),
    "Cozinha": (150, 930),
    "Administrador da matriz": (1350, 250),
    "Gerente": (1350, 580),
    "Serviço de pagamento\n(sistema externo)": (750, 1075),
}

links = [
    ("Cliente", ["UC01", "UC02", "UC03", "UC04", "UC05", "UC06", "UC07"]),
    ("Atendente", ["UC01", "UC02", "UC08", "UC09", "UC12"]),
    ("Cozinha", ["UC08", "UC09"]),
    ("Gerente", ["UC12", "UC13", "UC14", "UC15", "UC16"]),
    ("Administrador da matriz", ["UC16", "UC17", "UC18", "UC19"]),
    ("Serviço de pagamento\n(sistema externo)", ["UC10", "UC11"]),
]


def edge(cx, cy, tx, ty, a=EW / 2, b=EH / 2):
    """Ponto na borda da elipse (cx,cy) na direção de (tx,ty)."""
    dx, dy = tx - cx, ty - cy
    if dx == 0 and dy == 0:
        return cx, cy
    t = 1 / math.sqrt((dx / a) ** 2 + (dy / b) ** 2)
    return cx + dx * t, cy + dy * t


def actor_anchor(name, tx, ty):
    ax, ay = actors[name]
    if "Serviço" in name:
        return ax, ay - 40
    if name == "Atendente" and ty == 770:
        return ax + 28, 770
    return (ax + 28 if tx > ax else ax - 28), ay


out = []
out.append(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" font-family="Arial, Helvetica, sans-serif">')
out.append(f'<rect width="{W}" height="{H}" fill="#ffffff"/>')
out.append(f'<text x="{W/2}" y="34" font-size="22" font-weight="bold" text-anchor="middle" fill="#222">Diagrama de Casos de Uso — API Raízes do Nordeste</text>')
out.append(f'<rect x="{BX0}" y="{BY0}" width="{BX1-BX0}" height="{BY1-BY0}" fill="#fbf8f1" stroke="#555" stroke-width="1.6" rx="6"/>')
out.append(f'<text x="{BX0+14}" y="{BY0+26}" font-size="15" font-weight="bold" fill="#444">Sistema: API Raízes do Nordeste</text>')

# associações ator-caso de uso
for name, ucs in links:
    for k in ucs:
        cx, cy, _ = uc[k]
        ax, ay = actor_anchor(name, cx, cy)
        ex, ey = edge(cx, cy, ax, ay)
        out.append(f'<line x1="{ax:.1f}" y1="{ay:.1f}" x2="{ex:.1f}" y2="{ey:.1f}" stroke="#7a7a7a" stroke-width="1.1"/>')

# casos de uso
for k, (cx, cy, t) in uc.items():
    out.append(f'<ellipse cx="{cx}" cy="{cy}" rx="{EW/2}" ry="{EH/2}" fill="#fff4dc" stroke="#9a6b16" stroke-width="1.5"/>')
    out.append(f'<text x="{cx}" y="{cy-4}" font-size="11" fill="#9a6b16" text-anchor="middle">{k}</text>')
    out.append(f'<text x="{cx}" y="{cy+13}" font-size="14" fill="#222" text-anchor="middle">{t}</text>')

# atores (boneco UML)
for name, (ax, ay) in actors.items():
    hy = ay - 40
    out.append(f'<g stroke="#222" stroke-width="2" fill="none">'
               f'<circle cx="{ax}" cy="{hy}" r="11" fill="#fff"/>'
               f'<line x1="{ax}" y1="{hy+11}" x2="{ax}" y2="{ay+12}"/>'
               f'<line x1="{ax-20}" y1="{hy+24}" x2="{ax+20}" y2="{hy+24}"/>'
               f'<line x1="{ax}" y1="{ay+12}" x2="{ax-16}" y2="{ay+36}"/>'
               f'<line x1="{ax}" y1="{ay+12}" x2="{ax+16}" y2="{ay+36}"/></g>')
    for j, part in enumerate(name.split("\n")):
        out.append(f'<text x="{ax}" y="{ay+56+j*17}" font-size="15" font-weight="bold" fill="#222" text-anchor="middle">{part}</text>')

out.append("</svg>")
open("casos-de-uso.svg", "w", encoding="utf-8").write("\n".join(out))
print("ok")
