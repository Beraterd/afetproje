# Running the Frontend

This guide describes how to run the Istanbul Disaster Coordination Platform Frontend.

## Prerequisites

- Node.js (v18 or higher recommended)
- npm (v9 or higher recommended)
- Backend running locally (or pointing to a deployed backend)

## Setup Steps

1. **Install dependencies:**

   ```bash
   npm install
   ```

2. **Configure Environment Variables:**

   Make sure to configure your environment variables. 
   An example file is provided in `.env.example`.
   You can copy it to `.env.development`:

   ```bash
   cp .env.example .env.development
   ```

   The default configurations should look like this:
   ```env
   VITE_API_BASE_URL=http://localhost:8080
   VITE_MAP_TILE_URL=https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png
   VITE_MAP_ATTRIBUTION="&copy; OpenStreetMap contributors"
   VITE_SESSION_STORAGE_KEY=afet_coord_rt
   ```

3. **Start the Development Server:**

   ```bash
   npm run dev
   ```

   The application will become accessible at `http://localhost:5173`.

## Building for Production

To build the application for production, run:

```bash
npm run build
```

This will create a `dist` folder containing the compiled static assets. You can test the production build locally using `vite preview`:

```bash
npm run preview
```

## Project Roles

The frontend is an RBAC system. Based on backend specifications, log in with an account having one of the following roles to test varying features:

- `ADMIN` - Full access to all features and map views.
- `DISTRICT_COORDINATOR` / `NEIGHBORHOOD_COORDINATOR` - Local event access.
- `VOLUNTEER` - General access for discovering and joining events, uploading documents.

## Technology Stack

- **Framework**: React 18 + TypeScript + Vite
- **Routing**: React Router v6
- **Data Fetching**: Tanstack Query (React Query) v5 + Axios
- **State Management**: Zustand
- **Styling**: Tailwind CSS
- **Forms**: React Hook Form + Zod validation
- **Maps**: React-Leaflet
