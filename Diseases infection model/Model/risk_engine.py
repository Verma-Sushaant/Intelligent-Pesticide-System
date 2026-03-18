def calculate_infection_risk(base_prob, crop, temp, humidity, soil):
    risk = base_prob

    if crop == "wheat":
        if humidity > 70:
            risk += 10
        if temp > 30:
            risk += 5

    elif crop == "rice":
        if soil > 60:
            risk += 15
        if humidity > 75:
            risk += 10

    elif crop == "corn":
        if humidity > 65:
            risk += 10
        if temp > 32:
            risk += 5

    elif crop == "potato":
        if humidity > 80:
            risk += 15
        if temp < 18:
            risk += 10   # late blight risk

    elif crop == "sugarcane":
        if humidity > 70:
            risk += 10
        if soil < 30:
            risk += 5

    risk = min(100, risk)

    if risk <= 30:
        severity = "Low"
    elif risk <= 60:
        severity = "Medium"
    else:
        severity = "High"

    return int(risk), severity
